package net.ddns.endercrypt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class DiscordListener extends ListenerAdapter
{
	private static final Gson gson = new Gson();

	private static final int MAX_SIZE = (1000 * 1000 * 8); // 8mb (lol, not 1024..)

	private static String headerString = "$TRANSFER$";

	private static String fileFolderEmoji = "📁";

	public DiscordListener(@SuppressWarnings("unused") Discord discord)
	{
		// do nothing
	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event)
	{
		User author = event.getAuthor();
		boolean isMe = event.getJDA().getSelfUser().equals(author);
		Message message = event.getMessage();
		PrivateChannel channel = event.getChannel();
		User pmUser = channel.getUser();

		if (isMe && message.getContentRaw().equalsIgnoreCase("transfer"))
		{
			try
			{
				Optional<File> optionalFile = Util.promptOpen("Choose File/Folder to send to " + pmUser.getName());
				if (optionalFile.isPresent() == false)
				{
					return;
				}
				File file = optionalFile.get();
				System.out.println("To send: " + file);

				// zip
				File tempZipFile = Util.generateTempFile();
				compressFile(file, tempZipFile);

				// upload file
				uploadFile(channel, tempZipFile, file.getName());

				// delete file
				tempZipFile.delete();
			}
			catch (Exception ex)
			{
				Util.panic(ex);
			}
		}
	}

	private static void compressFile(File input, File output) throws ZipException
	{
		ZipParameters parameters = new ZipParameters();
		parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
		parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);

		ZipFile zipFile = new ZipFile(output);

		if (input.isFile())
		{
			zipFile.addFile(input, parameters);
		}
		if (input.isDirectory())
		{
			zipFile.addFolder(input, parameters);
		}
	}

	private static void uploadFile(PrivateChannel channel, File file, String filename)
	{
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);

		root.addProperty("filename", filename);

		long length = file.length();
		root.addProperty("length", length);
		int pieces = (int) Math.ceil((double) length / MAX_SIZE);
		System.out.println("Bytes: " + length + " Pieces: " + pieces);

		try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file)))
		{
			JsonArray piecesArray = new JsonArray();
			root.add("pieces", piecesArray);

			byte[] bytes = new byte[MAX_SIZE];
			int piece = 0;
			while (true)
			{
				piece++;
				int length_read = input.read(bytes, 0, MAX_SIZE);
				if (length_read == -1)
				{
					break;
				}
				System.out.println("Uploading part: " + piece + " size: " + length_read);

				byte[] uploadArray = bytes;
				if (length_read < MAX_SIZE)
				{
					uploadArray = Arrays.copyOfRange(uploadArray, 0, length_read);
				}
				Message uploadMessage = channel.sendFile(uploadArray, filename + ".part." + piece).complete();
				piecesArray.add(uploadMessage.getIdLong());
			}
			System.out.println("Done!");

			String jsonRoot = gson.toJson(root);
			String base64Json = Base64.getEncoder().encodeToString(jsonRoot.getBytes());
			Message message = channel.sendMessage(headerString + base64Json + "\n\n" + channel.getUser().getAsMention() + " if you have " + DiscordTransmissionWizard.name + " installed and running, press the :FileFolder: " + fileFolderEmoji + " emote reaction").complete();
			message.addReaction(fileFolderEmoji).complete();
		}
		catch (IOException e)
		{
			Util.panic(e);
		}
	}

	@Override
	public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event)
	{
		User user = event.getUser();
		boolean isMe = event.getJDA().getSelfUser().equals(user);
		long messageId = event.getMessageIdLong();
		PrivateChannel channel = event.getChannel();
		String reactionString = event.getReactionEmote().getName();

		if (isMe && reactionString.equals(fileFolderEmoji))
		{
			Message message = channel.getHistoryAround(messageId, 1).complete().getMessageById(messageId);
			String rawMessage = message.getContentRaw();
			if (rawMessage.startsWith(headerString) && message.getAuthor().equals(event.getJDA().getSelfUser()) == false)
			{
				int firstNewlineIndex = rawMessage.indexOf("\n");
				String base64 = rawMessage.substring(headerString.length(), firstNewlineIndex);
				System.out.println(base64);
				String rawJson = new String(Base64.getDecoder().decode(base64));
				System.out.println("received json: " + rawJson);
				JsonObject jsonRoot = gson.fromJson(rawJson, JsonObject.class);
				handleControlMessage(channel, jsonRoot);
			}
		}
	}

	private static void handleControlMessage(PrivateChannel channel, JsonObject jsonRoot)
	{
		// check version
		final int expectedVersion = 1;
		int version = jsonRoot.get("version").getAsInt();
		System.out.println("version: " + version);
		if (version != expectedVersion)
		{
			Util.failure("wrong version", "received version " + version + " but you are running " + expectedVersion);
			return;
		}
		// act
		String filename = jsonRoot.get("filename").getAsString();
		long length = jsonRoot.get("length").getAsLong();
		System.out.println("filename: " + filename + " (" + length + " bytes)");
		Optional<File> optionalFolder = Util.promptSaveFolder("Choose where to save " + filename, filename);
		if (optionalFolder.isPresent() == false)
		{
			return;
		}
		File destination = optionalFolder.get();
		System.out.println("destination: " + destination);
		if (destination.isDirectory() == false)
		{
			Util.failure("Warning!", destination.toString() + " is not a directory!");
		}

		// pieces array
		JsonArray jsonPieces = jsonRoot.get("pieces").getAsJsonArray();
		Stream<JsonElement> piecesStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonPieces.iterator(), Spliterator.ORDERED), false);
		List<Message> messageList = piecesStream.mapToLong(e -> e.getAsLong()).mapToObj(id -> channel.getHistoryAround(id, 1).complete().getMessageById(id)).collect(Collectors.toList());

		System.out.println("pieces: " + messageList.stream().map(m -> m.toString()).collect(Collectors.joining(", ")));

		// download
		File tempZip = Util.generateTempFile();
		try
		{
			downloadFile(channel, messageList, tempZip);
		}
		catch (IOException ex)
		{
			Util.failure(ex);
			return;
		}

		// un-zip

		try
		{
			decompressFile(tempZip, destination);
		}
		catch (ZipException ex)
		{
			Util.failure(ex);
			return;
		}

		tempZip.delete();

		System.out.println("Download of " + filename + "\nTo" + destination + " is finished!");
		JOptionPane.showMessageDialog(null, "Download of " + filename + "\nTo" + destination + " is finished!");
	}

	private static void downloadFile(PrivateChannel channel, List<Message> messages, File destination) throws FileNotFoundException, IOException
	{
		Message progressMessage = channel.sendMessage("Starting download...").complete();

		try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(destination)))
		{
			int total = messages.size();
			int count = 0;
			for (Message message : messages)
			{
				count++;
				Attachment attachment = message.getAttachments().iterator().next();
				System.out.println("Downloading: " + attachment.getFileName());
				progressMessage.editMessage("Currently downloading " + attachment.getFileName() + " ( " + Math.round(100.0 / total * count) + "% )").complete();
				File tempFile = Util.generateTempFile();
				attachment.download(tempFile);
				try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(tempFile)))
				{
					IOUtils.copy(input, output);
				}
				tempFile.delete();
			}
		}
		progressMessage.editMessage("Download completed!").complete();
	}

	private static void decompressFile(File inputZipFile, File outputDirectory) throws ZipException
	{
		ZipFile zipFile = new ZipFile(inputZipFile);
		zipFile.extractAll(outputDirectory.toString());
	}

	@Override
	public void onPrivateMessageReactionRemove(PrivateMessageReactionRemoveEvent event)
	{
		User user = event.getUser();
		boolean isMe = event.getJDA().getSelfUser().equals(user);
		long messageId = event.getMessageIdLong();
		PrivateChannel channel = event.getChannel();
		String reactionString = event.getReactionEmote().getName();

		if (isMe && reactionString.equals(fileFolderEmoji))
		{
			Message message = channel.getHistoryAround(messageId, 1).complete().getMessageById(messageId);
			String rawMessage = message.getContentRaw();
			if (rawMessage.startsWith(headerString) && message.getAuthor().equals(event.getJDA().getSelfUser()))
			{
				int firstNewlineIndex = rawMessage.indexOf("\n");
				String base64 = rawMessage.substring(headerString.length(), firstNewlineIndex);
				System.out.println(base64);
				String rawJson = new String(Base64.getDecoder().decode(base64));
				System.out.println("delete based on json: " + rawJson);
				JsonObject jsonRoot = gson.fromJson(rawJson, JsonObject.class);
				deleteControlMessage(channel, jsonRoot);
				message.delete().complete();
			}
		}
	}

	private static void deleteControlMessage(PrivateChannel channel, JsonObject jsonRoot)
	{
		// check version
		final int expectedVersion = 1;
		int version = jsonRoot.get("version").getAsInt();
		System.out.println("version: " + version);
		if (version != expectedVersion)
		{
			Util.failure("wrong version", "received version " + version + " but you are running " + expectedVersion);
			return;
		}

		// pieces array
		JsonArray jsonPieces = jsonRoot.get("pieces").getAsJsonArray();
		Stream<JsonElement> piecesStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonPieces.iterator(), Spliterator.ORDERED), false);
		List<Message> messageList = piecesStream.mapToLong(e -> e.getAsLong()).mapToObj(id -> channel.getHistoryAround(id, 1).complete().getMessageById(id)).collect(Collectors.toList());

		// delete pieces
		for (Message message : messageList)
		{
			message.delete().queue(v -> {
				// ignore
			}, ex -> {
				ex.printStackTrace();
			});
		}
	}
}
