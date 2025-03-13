import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;



public class ServeurSimple
{
	private static final int portNumber = 9000;
	private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
	private static int clientCounter = 1;

	public static void main(String[] args)
	{
		try
		{
			ServerSocket serverSocket = new ServerSocket(portNumber);
			ServerSocket fileServerSocket = new ServerSocket(portNumber + 1);
			System.out.println("Serveur démarré. En attente de clients...");
			while (true)
			{
				Socket clientSocket = serverSocket.accept();
				System.out.println("Client connecté : " + clientSocket.getInetAddress());
				String username = "Utilisateur-" + clientCounter++;
				ClientHandler clientHandler = new ClientHandler(clientSocket, username);
				clients.add(clientHandler);
				new Thread(clientHandler).start();

				Socket fileSocket = fileServerSocket.accept();
				new Thread(new FileHandler(fileSocket)).start();
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void broadcastMessage(String message)
	{
		synchronized (clients)
		{
			for (ClientHandler client : clients)
			{
				client.sendMessage(message);
			}
		}
	}

	public static void broadcastFile(File file)
	{
		synchronized (clients)
		{
			for (ClientHandler client : clients)
			{
				client.sendFile(file);
			}
		}
	}

	public static void removeClient(ClientHandler clientHandler)
	{
		synchronized (clients)
		{
			clients.remove(clientHandler);
			reorderClients();
		}
	}

	private static void reorderClients()
	{
		for (int i = 0; i < clients.size(); i++)
		{
			clients.get(i).setCodeName("Utilisateur-" + (i + 1));
			clients.get(i).sendMessage("Votre nouveau nom de code est : Utilisateur-" + (i + 1));
		}
		clientCounter = clients.size() + 1;
	}
}

class ClientHandler implements Runnable
{
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;
	private String codeName;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public ClientHandler(Socket socket, String codeName)
	{
		this.clientSocket = socket;
		this.codeName = codeName;
	}

	@Override
	public void run()
	{
		try
		{
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			out.println("Bienvenue sur le serveur, votre nom de code est : " + codeName);
			String receivedMessage;
			while ((receivedMessage = in.readLine()) != null)
			{
				if (receivedMessage.isEmpty())
				{
					break;
				}

				// Gestion de la commande /rename
				if (receivedMessage.startsWith("/rename "))
				{
					String newName = receivedMessage.substring(8).trim();
					if (!newName.isEmpty())
					{
						setCodeName(newName);
						ServeurSimple.broadcastMessage(codeName + " a changé son nom en : " + newName);
					}
					continue;
				}

				// Format du message envoyé
				String timestamp = dateFormat.format(new Date());
				String formattedMessage = "[" + timestamp + "] " + codeName + " : " + receivedMessage;
				System.out.println(formattedMessage);
				ServeurSimple.broadcastMessage(formattedMessage);
			}
			closeConnection();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendMessage(String message)
	{
		synchronized (this)
		{
			if (out != null)
			{
				// Envoie le message sur une nouvelle ligne
				out.println(message);
				out.flush();
			}
		}
	}

	public void sendFile(File file)
	{
		try
		{
			Socket fileSocket = new Socket(clientSocket.getInetAddress(), clientSocket.getPort() + 1);
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
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void setCodeName(String newCodeName)
	{
		this.codeName = newCodeName;
	}

	private void closeConnection()
	{
		try
		{
			in.close();
			out.close();
			clientSocket.close();
			ServeurSimple.removeClient(this);
			// Réinitialisation du nom de l'utilisateur si déconnecté
			ServeurSimple.broadcastMessage(codeName + " s'est déconnecté.");
			System.out.println("Client déconnecté : " + codeName);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}

class FileHandler implements Runnable
{
	private Socket fileSocket;

	public FileHandler(Socket socket)
	{
		this.fileSocket = socket;
	}

	@Override
	public void run()
	{
		try
		{
			BufferedInputStream bis = new BufferedInputStream(fileSocket.getInputStream());
			File receivedFile = new File("received_" + System.currentTimeMillis());
			FileOutputStream fos = new FileOutputStream(receivedFile);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = bis.read(buffer)) != -1)
			{
				fos.write(buffer, 0, bytesRead);
			}
			fos.close();
			bis.close();
			fileSocket.close();
			ServeurSimple.broadcastFile(receivedFile);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}