import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import com.erg.reco.Poi;

public class Master extends Thread {
	Socket connection;
	int id;
	ObjectInputStream in;
	ObjectOutputStream out;
	int cores;
	long available_memory;
	static int frag_x;
	static int frag_y;
	ArrayList<Poi> pois = new ArrayList<Poi>();// workerlist

	public Master() {
		connection = null;
	}

	public Master(Socket connection, int id) {
		this.connection = connection;
		this.id = id;
	}

	public void run() { // ola t thread exoun run
		try {
			out = new ObjectOutputStream(connection.getOutputStream());
			in = new ObjectInputStream(connection.getInputStream());

			// ===========================================
			// TYPE
			// 0 is worker
			// 1 is client
			// ===========================================
			if (in.readInt() == 0)// worker
			{
				out.writeInt(this.id);
				out.flush(); // set id
				cores = in.readInt();
				available_memory = in.readLong();
				// ===================================
				System.out.println("=================");
				System.out.println("Worker [" + id + "]");
				System.out.println("Cores [" + cores + "]");
				System.out.println("Memory [" + available_memory + "]");
				System.out.println("=================");

				while (mainApp.works_done < mainApp.max_works) {
					if (move()) {
						initialize();
					}
				}
				closeConnenction();

			} else// 1 is client
			{
				initializeClient();
			}
		} catch (Exception e) {
			System.out.println("Error -- initialize in / out exception id: " + id + " .");
			System.out.println(e.getMessage());

		}
	}

	private synchronized boolean move() {
		if (mainApp.flag == false) {
			double last_error = -1;
			double new_error;
			new_error = mainApp.calculateError();
			System.out.println("Error= " + new_error + " ~ " + Math.abs(new_error - last_error));
			last_error = new_error;
			mainApp.flag = true;
			return false;
		} else {
			return true;
		}
	}

	private void initializeClient() throws IOException// client
	{
		System.out.println("Client connected");
		// get best pois

		int user = in.readInt();
		// System.out.println(user);
		if (user < 0 || user > mainApp.y)
			user = 0;
		calculateBestPoisForUser(user, mainApp.w); // best 5
		// send pois as an array

		System.out.println("Sending pois");
		// System.out.println(pois);

		out.writeObject(pois);

		System.out.println("Done sending pois");
		out.flush();
		connection.close();
		return;
	}

	public void calculateBestPoisForUser(int value1, int value2) {
		System.out.println("Calculating best Pois for user: " + value1 + ".");

		RealMatrix Rui = MatrixUtils.createRealMatrix(mainApp.R.getRowDimension(), mainApp.R.getColumnDimension());

		RealMatrix X_u = MatrixUtils.createRealMatrix(mainApp.X.getColumnDimension(), 1);

		RealMatrix Y_i = MatrixUtils.createRealMatrix(mainApp.Y.getColumnDimension(), 1);

		RealMatrix temp;
		RealMatrix temp2 = MatrixUtils.createRealMatrix(1, value2);

		for (int i = 0; i < mainApp.R.getRowDimension(); i++) {

			for (int k = 0; k < mainApp.X.getColumnDimension(); k++) {
				X_u.setEntry(k, 0, mainApp.X.getEntry(i, k));
			}
			for (int j = 0; j < mainApp.R.getColumnDimension(); j++) {

				for (int l = 0; l < mainApp.Y.getColumnDimension(); l++) {
					Y_i.setEntry(l, 0, mainApp.Y.getEntry(j, l));
				}

				temp = X_u.transpose().multiply(Y_i);
				// System.out.println(temp.getEntry(0,0));
				Rui.setEntry(i, j, temp.getEntry(0, 0));
			}
		}

		double[] temp3 = Rui.getRow(value1);

		double max = 0;
		int pointer = 0;

		for (int i = 0; i < value2; i++) {
			for (int j = 0; j < Rui.getColumnDimension(); j++) {
				if (max < temp3[j]) {
					max = temp3[j];
					pointer = j;
					// System.out.println(pointer +"POI");
				}
			}
			// pointer is id

			for (int z = 0; z < mainApp.listofpois.size(); z++) {
				if (mainApp.listofpois.get(z).getId() == pointer) {
					pois.add(mainApp.listofpois.get(z));
					// System.out.println(mainApp.listofpois.get(z).getName());
				}
			}

			// System.out.println("End of this loop");
			temp2.setEntry(0, i, pointer);
			temp3[pointer] = 0;
			max = 0;
			pointer = 0;
		}
		// for (int z = 0; z < pois.size(); z++) {
		// System.out.println("-"+pois.get(z).getName());
		//
		// }

		// System.out.println("The best " + value2 + " POIs for User: " + value1 + "
		// are: ");
		//
		// for(int i=0; i<value2-1; i++) {
		// System.out.println("POI with ID: " + temp2.getEntry(0,i) + ", ");
		//
		// }
		// System.out.println("POI with ID: " + temp2.getEntry(0,value2-1));
	}

	public void closeConnenction() throws IOException {
		out.writeInt(0);
		out.flush();
		System.out.println("Closing connection with worker [" + id + "]");
	}

	public void initialize() throws Exception {
		int first, last;
		out.writeInt(1);
		out.flush();

		out.writeDouble(mainApp.lamda);
		out.flush();

		out.writeObject(mainApp.distY());
		out.flush();

		RealMatrix Y_tmp = (RealMatrix) in.readObject();
		first = in.readInt();
		last = in.readInt();

		mainApp.updateY(first, last, Y_tmp);

		out.writeObject(mainApp.distX());
		out.flush();

		RealMatrix X_tmp = (RealMatrix) in.readObject();
		first = in.readInt();
		last = in.readInt();

		mainApp.updateX(first, last, X_tmp);

		// System.out.println("done updating X , Y");

	}

	public String toString() {
		return "This is a Server object\n" + "Socket: " + this.connection + " .";

	}
}
