package net.ddns.endercrypt;

import javax.security.auth.login.LoginException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class DiscordTransmissionWizard
{
	public static final String name = DiscordTransmissionWizard.class.getSimpleName();

	private static Discord discord;

	public static void main(String[] args)
	{
		// look and feel
		try
		{
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		}
		catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex)
		{
			// ignore
		}

		// credits
		JOptionPane.showMessageDialog(null, ""
				+ "Created by EnderCrypt (Magnus Gunnarsson)\n"
				+ "Version 1\n"
				+ "March 2018\n", DiscordTransmissionWizard.class.getSimpleName(), JOptionPane.INFORMATION_MESSAGE);

		// discord token
		String discordToken = null;

		// arg
		if (args.length == 1)
		{
			discordToken = args[0];
		}

		// show discord token prompt
		if (discordToken == null)
		{
			discordToken = JOptionPane.showInputDialog(null, ""
					+ "To login you'll need your discord token\n"
					+ "To get the token (in google chrome)\n"
					+ "Login to discord (webpage)\n"
					+ "Right click 'Inspect Elements'\n"
					+ "Click the 'Application' tab\n"
					+ "Local Storage -> https://discordapp.com\n"
					+ "Copy and paste the token here", "Discord Login", JOptionPane.QUESTION_MESSAGE);
			if (discordToken == null)
			{
				System.exit(0);
			}
		}
		discordToken = discordToken.trim();
		if (discordToken.startsWith("\"") && discordToken.endsWith("\""))
		{
			discordToken = discordToken.substring(1, discordToken.length() - 1);
		}
		System.out.println("input: " + discordToken);
		if (discordToken.length() != 59)
		{
			Util.panic("Invalid Token", "This token does not look like a discord token");
		}

		// login to discord
		try
		{
			discord = new Discord(discordToken);
			discord.addEventListener(new DiscordListener(discord));
			System.out.println("Login successfull!");
			JOptionPane.showMessageDialog(null, ""
					+ "Login successfull!\n"
					+ "\n"
					+ "To send a file or folder, simply type: transfer\n"
					+ "in any private message channel\n", "Discord", JOptionPane.PLAIN_MESSAGE);
		}
		catch (LoginException e)
		{
			Util.panic("Login Failed!", "Failed to login to discord\n"
					+ "This most likely is because the provided token\n"
					+ "Was invalid/wrong");
		}
		catch (InterruptedException e)
		{
			Util.panic(e);
		}
	}
}
