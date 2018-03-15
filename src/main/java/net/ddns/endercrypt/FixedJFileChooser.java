package net.ddns.endercrypt;

import java.awt.Component;
import java.awt.HeadlessException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

@SuppressWarnings("serial")
public class FixedJFileChooser extends JFileChooser
{
	@Override
	public int showDialog(Component parent, String approveButtonText) throws HeadlessException
	{
		if (parent == null)
		{
			JFrame tempJFrame = new JFrame();
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				// ignore
			}
			int returnValue = super.showDialog(parent, approveButtonText);
			tempJFrame.dispose();
			return returnValue;
		}
		else
		{
			return super.showDialog(parent, approveButtonText);
		}
	}
}
