import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;



public class ClientSimple extends JFrame
{
	private JTextPane messagePane;
	private JTextField inputField;
	private JTextField addressField;
	private JTextField portField;
	private PrintWriter out;
	private Socket toServer;
	private BufferedReader in;

	public ClientSimple()
	{
		setTitle("Client Simple");
		setSize(400, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		messagePane = new JTextPane();
		messagePane.setEditable(false);
		add(new JScrollPane(messagePane), BorderLayout.CENTER);

		JPanel inputPanel = new JPanel(new BorderLayout());
		inputField = new JTextField();
		inputField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					String userMessage = inputField.getText();
					if (!userMessage.isEmpty())
					{
						out.println(userMessage);
						inputField.setText("");
					}
				}
			}
		});
		inputPanel.add(inputField, BorderLayout.CENTER);

		JButton sendButton = new JButton("Envoyer");
		sendButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String userMessage = inputField.getText();
				if (!userMessage.isEmpty())
				{
					out.println(userMessage);
					inputField.setText("");
				}
			}
		});
		inputPanel.add(sendButton, BorderLayout.EAST);

		JButton fileButton = new JButton("Envoyer Fichier");
		fileButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser fileChooser = new JFileChooser();
				int returnValue = fileChooser.showOpenDialog(null);
				if (returnValue == JFileChooser.APPROVE_OPTION)
				{
					File selectedFile = fileChooser.getSelectedFile();
					sendFile(selectedFile);
				}
			}
		});
		inputPanel.add(fileButton, BorderLayout.WEST);

		add(inputPanel, BorderLayout.SOUTH);

		JPanel connectionPanel = new JPanel(new GridLayout(1, 3));
		addressField = new JTextField("Adresse du serveur");
		portField = new JTextField("Port");
		JButton connectButton = new JButton("Se connecter");
		connectButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String hostName = addressField.getText();
				int portNumber;
				try
				{
					portNumber = Integer.parseInt(portField.getText());
					connectToServer(hostName, portNumber);
				} catch (NumberFormatException ex)
				{
					appendMessage("Le port doit être un numéro valide.", false);
				}
			}
		});
		connectionPanel.add(addressField);
		connectionPanel.add(portField);
		connectionPanel.add(connectButton);
		add(connectionPanel, BorderLayout.NORTH);
	}

	private void connectToServer(String hostName, int portNumber)
	{
		try
		{
			if (toServer != null && !toServer.isClosed())
			{
				toServer.close();
			}
			toServer = new Socket(hostName, portNumber);
			appendMessage("Connecté à " + hostName, false);

			out = new PrintWriter(toServer.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(toServer.getInputStream()));

			// Thread pour lire les messages du serveur
			Thread readThread = new Thread(() -> {
				try
				{
					String serverMessage;
					while ((serverMessage = in.readLine()) != null)
					{
						appendMessage("Serveur (" + hostName + ") : " + serverMessage, true);
					}
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			});
			readThread.start();
		} catch (UnknownHostException e)
		{
			appendMessage(hostName + " : Hôte inconnu.", false);
		} catch (IOException e)
		{
			appendMessage(hostName + " : Impossible de se connecter.", false);
		}
	}

	private void sendFile(File file)
	{
		try
		{
			Socket fileSocket = new Socket(toServer.getInetAddress(), toServer.getPort() + 1);
			BufferedOutputStream bos = new BufferedOutputStream(fileSocket.getOutputStream());
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1)
			{
				bos.write(buffer, 0, bytesRead);
			}
			bos.close();
			fis.close();
			fileSocket.close();
			appendMessage("Fichier envoyé : " + file.getName(), false);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void appendMessage(String message, boolean isPseudo)
	{
		StyledDocument doc = messagePane.getStyledDocument();
		SimpleAttributeSet keyWord = new SimpleAttributeSet();
		if (isPseudo)
		{
			StyleConstants.setForeground(keyWord, Color.RED);
		}
		else
		{
			StyleConstants.setForeground(keyWord, Color.BLACK);
		}
		try
		{
			doc.insertString(doc.getLength(), message + "\n", keyWord);
		} catch (BadLocationException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() -> {
			ClientSimple client = new ClientSimple();
			client.setVisible(true);
		});
	}
}