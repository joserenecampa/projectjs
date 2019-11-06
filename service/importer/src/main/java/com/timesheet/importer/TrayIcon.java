package com.timesheet.importer;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.JOptionPane;

public class TrayIcon {

	public static Runnable runner;

	public TrayIcon() {
		try {
			this.makeTrayIcon();
		} catch (Exception e) {
			String msg = "Problem: " + e.getMessage();
			JOptionPane.showMessageDialog(null, msg);
			System.exit(1);
		}
	}

	private void waitCanMakeTrayIcon() throws Exception {
		Long startTime = System.currentTimeMillis();
		while (!SystemTray.isSupported()) {
			Long endTime = System.currentTimeMillis();
			Long diff = endTime - startTime;
			if (diff > 60 * 1000) {
				throw new Exception("Operation System doesn't support tray icon!");
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public void makeTrayIcon() throws Exception {
		waitCanMakeTrayIcon();
		runner = new Runnable() {
			public void run() {
				if (SystemTray.isSupported()) {
					final SystemTray tray = SystemTray.getSystemTray();
					URL urlImagem = this.getClass().getClassLoader().getResource("timesheet.png");
					urlImagem = this.getClass().getClassLoader().getResource("com/timesheet/importer/timesheet.png");
					Image image = Toolkit.getDefaultToolkit().getImage(urlImagem);
					PopupMenu popup = new PopupMenu();
					final java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image, "Importer", popup);
					trayIcon.setImageAutoSize(true);
					trayIcon.setToolTip("Importer");
					MenuItem closeMenu = new MenuItem("Close");
					closeMenu.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							System.exit(0);
						}
					});
					popup.add(closeMenu);
					try {
						tray.add(trayIcon);
					} catch (AWTException e) {
					}
				} else {
					JOptionPane.showMessageDialog(null, "Operation System doesn't support tray icon!");
				}
			}
		};
		EventQueue.invokeLater(runner);
	}
}