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
				+ "Version 1.3\n"
				+ "March 2018\n"
				+ "Open Source: https://github.com/EnderCryptAlt/DiscordMegaTransfer", DiscordTransmissionWizard.class.getSimpleName(), JOptionPane.INFORMATION_MESSAGE);

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
					+ "To proceed you need to get a discord token\n"
					+ "you can do this in any browser, but i'll explain it for chrome\n"
					+ "1. Login to discord (website)\n"
					+ "2. Click the chrome menu\n"
					+ "3. Click 'more tools'\n"
					+ "4. Click 'Developer tools'\n"
					+ "5. Click the 'Application' tab\n"
					+ "6. Local Storage -> https://discordapp.com\n"
					+ "7. Find the 'token' field (you may need to scroll down)\n"
					+ "8. Copy and paste the token here", "Discord Login", JOptionPane.QUESTION_MESSAGE);
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
