package net.ddns.endercrypt;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class Discord
{
	private JDA jda;

	public Discord(String token) throws LoginException, InterruptedException
	{
		JDABuilder jdaBuilder = new JDABuilder(AccountType.CLIENT);
		jdaBuilder.setToken(token);
		jda = jdaBuilder.buildBlocking();
	}

	public JDA getJda()
	{
		return jda;
	}

	public void addEventListener(Object object)
	{
		getJda().addEventListener(object);
	}
}
