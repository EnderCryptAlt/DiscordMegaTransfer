package net.ddns.endercrypt;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.lang.exception.ExceptionUtils;

public class Util
{
	// file //

	public static File generateTempFile()
	{
		return generateTempFile("temp");
	}

	public static File generateTempFile(String extension)
	{
		return new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + "." + extension);
	}

	// prompt //

	public static Optional<File> promptOpen(String title)
	{
		JFileChooser fileChooser = new FixedJFileChooser();
		fileChooser.setDialogTitle(title);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int returnVal = fileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			return Optional.of(fileChooser.getSelectedFile());
		}
		return Optional.empty();
	}

	public static Optional<File> promptSaveFolder(String title, String filename)
	{
		JFileChooser fileChooser = new FixedJFileChooser();
		fileChooser.setName(filename);
		fileChooser.setDialogTitle(title);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setAcceptAllFileFilterUsed(false);
		int returnVal = fileChooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			return Optional.of(fileChooser.getCurrentDirectory());
		}
		return Optional.empty();
	}

	// issues //

	public static void failure(Throwable e)
	{
		failure("Error occured!", ExceptionUtils.getStackTrace(e));
	}

	public static void failure(String title, String message)
	{
		show("Failure", title, message);
	}

	public static void panic(Throwable e)
	{
		panic("Error occured!", ExceptionUtils.getStackTrace(e));
	}

	public static void panic(String title, String message)
	{
		show("Panic", title, message);
		System.exit(1);
	}

	private static void show(String type, String title, String message)
	{
		System.err.println(type + ": " + message);
		JOptionPane.showMessageDialog(null, type + "!\n" + message, title, JOptionPane.ERROR_MESSAGE);
	}
}
