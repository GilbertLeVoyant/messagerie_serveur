import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServeurSimple
{
	private static final int portNumber = 9000;
	private static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
	private static int clientCounter = 1;

	public static void main(String[] args)
	{
		try
		{
			ServerSocket serverSocket = new ServerSocket(portNumber);
			System.out.println("Serveur démarré. En attente de clients...");

			while (true)
			{
				Socket clientSocket = serverSocket.accept();
				System.out.println("Client connecté : " + clientSocket.getInetAddress());

				ClientHandler clientHandler = new ClientHandler(clientSocket, "Utilisateur-" + clientCounter++);
				clients.add(clientHandler);
				new Thread(clientHandler).start();
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

	public static void removeClient(ClientHandler clientHandler)
	{
		synchronized (clients)
		{
			clients.remove(clientHandler);
		}
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
				out.println(message);
				out.flush();
			}
		}
	}

	private void closeConnection()
	{
		try
		{
			in.close();
			out.close();
			clientSocket.close();
			ServeurSimple.removeClient(this);
			System.out.println("Client déconnecté : " + codeName);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
