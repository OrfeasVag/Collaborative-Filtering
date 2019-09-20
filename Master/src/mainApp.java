import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import com.erg.reco.Poi;

//Master
public class mainApp {
	// kane enan pinaka me ola ta pois apo to json k meta analoga t id antigrafw to
	// poi sto array p 8a steilw sto client
	static RealMatrix R, X, Y, P, C, I;
	static int connection_counter = 0;
	static int x = 835;// Users
	static int y = 1692;// PoI
	static int k = 20;
	static int w = 5;// top k
	static int a = 40;
	static double lamda = 0.1;
	static ArrayList<Poi> listofpois = new ArrayList<Poi>();

	static ArrayList<Master> workerlist = new ArrayList<Master>();// workerlist
	static int works_done = 0;
	static int max_works = 5;
	static boolean flag = true;
	static int fragments = 7;
	static int fragment_counterX = 0;
	static int fragment_counterY = 0;

	// static RealMatrix[] fragmentedP_X = new RealMatrix[fragments];
	// static RealMatrix[] fragmentedP_Y = new RealMatrix[fragments];
	// static RealMatrix[] fragmentedC_X = new RealMatrix[fragments];
	// static RealMatrix[] fragmentedC_Y = new RealMatrix[fragments];

	static int[] fragmentX_first = new int[fragments];
	static int[] fragmentX_last = new int[fragments];
	static int[] fragmentY_first = new int[fragments];
	static int[] fragmentY_last = new int[fragments];

	public static void main(String[] args) {
		System.out.println("Launching Master Master . . .");
		readFile();// set R
		readJson();// set POI list
		setTableP();
		setTableC();
		setTableI(mainApp.k);
		X = MatrixUtils.createRealMatrix(mainApp.x, mainApp.k);
		Y = MatrixUtils.createRealMatrix(mainApp.y, mainApp.k);
		X = randTable(X);
		Y = randTable(Y);
		// fragmentP_X();
		// fragmentP_Y();
		// fragmentC_X();
		// fragmentC_Y();
		helper_fragX();
		helper_fragY();

		System.out.println("All tables loaded. ");

		openServer();
	}

	private static void helper_fragX() {
		int first = 0;
		int last = 0;
		for (int i = 0; i < fragments; i++) {
			first = last;// first is where the last one ended
			if (i == 0)// if length mod fragments != 0 o prwtos pairnei +1 stoixeio
			{
				first = 0;
				last = 0;
				if (X.getData().length % fragments != 0) {
					last = (X.getData().length / fragments) + 1;
				} else {
					last = X.getData().length / fragments;
				}
			} else {
				last = first + X.getData().length / fragments;
			}
			fragmentX_first[i] = first;
			fragmentX_last[i] = last;
		}
	}

	private static void helper_fragY() {
		int first = 0;
		int last = 0;
		for (int i = 0; i < fragments; i++) {
			first = last;// first is where the last one ended
			if (i == 0)// if length mod fragments != 0 o prwtos pairnei +1 stoixeio
			{
				first = 0;
				last = 0;
				if (Y.getData().length % fragments != 0) {
					last = (Y.getData().length / fragments) + 1;
				} else {
					last = Y.getData().length / fragments;
				}
			} else {
				last = first + Y.getData().length / fragments;
			}
			fragmentY_first[i] = first;
			fragmentY_last[i] = last;
		}
	}

	private static void openServer() {
		ServerSocket providerSocket;
		Socket connection = null;
		int id = 1;
		try {
			providerSocket = new ServerSocket(4200);
			while (true) {
				connection = providerSocket.accept();
				// System.out.println("Got a new connection...");
				Master t = new Master(connection, id);
				id++;
				workerlist.add(t);
				t.start();
			}
		} catch (IOException e) {
			System.out.println("Error -- Catch in openServer.");
			e.printStackTrace();
		}
	}

	private static void readFile() {
		double[][] pinakas = new double[x][y];
		for (int i = 0; i < x; i++) {
			for (int j = 0; j < y; j++) {
				pinakas[i][j] = 0;
			}
		}
		int row = 0, column = 0, value = 0;
		int count = 0;// 1-3 ari8mos le3ewn
		String line = "";
		File f = null;
		StringTokenizer st = null;
		String token = "";
		BufferedReader reader = null;
		// ================================== [ Load to file ]
		try {
			f = new File("input_matrix_non_zeros.csv");
		} catch (NullPointerException e) {
			System.err.println("File not found !");
		}
		// ================================== [ Load to reader ]
		try {
			reader = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			System.err.println("Error opening file!");
		}
		try {
			while (true) {// loop gia ka8e grammh
				line = reader.readLine();
				if (line == null) {
					break; // An file == null telos
				}
				st = new StringTokenizer(line);
				while (true) {// loop gia ka8e le3h
					count++;
					token = st.nextToken();
					// System.out.println(line);
					// System.out.println(token);
					if (token.contains(",")) { // fix otan einai ths morfhs
												// 12312, => 12312
						token = token.substring(0, token.length() - 1);
					}
					if (count == 1) {
						row = Integer.parseInt(token);
					} else if (count == 2) {
						column = Integer.parseInt(token);
					} else if (count == 3) {
						value = Integer.parseInt(token);
						count = 0;
						break;// phra k thn 3h timh kai twra allazw grammh
					}
				} // loop le3hs
					// System.out.println(
					// "Line: " + line_counter + " Row: " + row + " Column: " +
					// column + " Value: "
					// + value + " .");
					// edw ftiaxnw t antikeimeno Poi me ta row ,columt , value
					// kai to bazw sthn
					// lista ?
				pinakas[row][column] = value;

			} // loop grammhs
			System.out.println("Done reading file.");
		} // try
		catch (Exception e) {
			System.out.println("Exception while reading file.");
			System.out.println(e);
		}
		R = MatrixUtils.createRealMatrix(pinakas);
		// m = m.scalarAdd(0.01);
	}// readfile()

	private static void readJson() {
		int id = 0;
		String poi;
		double lat;
		double longit;
		String photo;
		String cat;
		String name;

		String line = "";
		File f = null;
		StringTokenizer st = null;
		String token = "";
		BufferedReader reader = null;
		// ================================== [ Load to file ]
		try {
			f = new File("POIs.json");
		} catch (NullPointerException e) {
			System.err.println("File not found !");
		}
		// ================================== [ Load to reader ]
		try {
			reader = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			System.err.println("Error opening file!");
		}
		try {
			line = reader.readLine();// first line is { skip it
			while (true) {// loop gia ka8e grammh
				line = reader.readLine();
				if (line == null) {
					break; // An file == null telos
				}
				st = new StringTokenizer(line);
				token = st.nextToken();

				if (line.contains("{")) {
					line = reader.readLine();
					st = new StringTokenizer(line);
					token = st.nextToken();// Poi
					token = st.nextToken();// actual poi
					token = token.substring(1, token.length() - 2);// clean " ",
					poi = token;// got poi
					// System.out.println(token);

					line = reader.readLine();
					st = new StringTokenizer(line);
					token = st.nextToken();// lat
					token = st.nextToken();// actual lat
					token = token.substring(0, token.length() - 1);// clean ,
					lat = Double.parseDouble(token);
					// System.out.println(token);

					line = reader.readLine();
					st = new StringTokenizer(line);
					token = st.nextToken();// long
					token = st.nextToken();// actual long
					token = token.substring(0, token.length() - 1);// clean ,
					longit = Double.parseDouble(token);
					// System.out.println(token);

					line = reader.readLine();
					st = new StringTokenizer(line);
					token = st.nextToken();// photos
					token = st.nextToken(",");// actual photos cut when ,
					token = token.substring(2, token.length() - 1);// clean _" "
					photo = token;
					// System.out.println(token);

					line = reader.readLine();
					st = new StringTokenizer(line);
					token = st.nextToken();// POI cat
					token = st.nextToken();// actual POI cat
					token = token.substring(1, token.length() - 2);// clean " ",
					cat = token;
					// System.out.println(token);

					line = reader.readLine();
					st = new StringTokenizer(line);
					token = st.nextToken();// POI name
					token = st.nextToken("");// actual POI name
					token = token.substring(2, token.length() - 1);// clean _"
					name = token;
					// System.out.println(token);

					// System.out.println("Adding poi " + id + " , " + name + " , " + lat + " , " +
					// longit + " , " + cat
					// + " , " + photo);

					listofpois.add(new Poi(id, name, lat, longit, cat, photo));
					// what about poi ?
					id++;
				} // if

			} // loop
		} // try
		catch (Exception e) {
			System.out.println("Error reading Json.");
		}

		// poi = (String) obj.get("POI");
		// lat = (double) obj.get("latidude");
		// longit = (double) obj.get("longitude");
		// photo = (String) obj.get("photos");
		// cat = (String) obj.get("POI_category_id");
		// name = (String) obj.get("POI_name");
		//
		System.out.println("Total Pois: " + (id - 1) + ".");
	}

	private static RealMatrix randTable(RealMatrix LU) {// bazei tuxaies times
														// sto pinaka LU

		RandomGenerator randomGenerator = new JDKRandomGenerator();
		randomGenerator.setSeed(1);
		for (int i = 0; i < LU.getRowDimension(); i++) {
			for (int j = 0; j < LU.getColumnDimension(); j++) {
				LU.setEntry(i, j, randomGenerator.nextDouble());
			}
		}
		return LU;
	}

	private static void setTableP() {// an o C exei timi megaluterh tou 0 sthn
										// 8esh i,j bazei thn timh 1 sthn 8esh
										// i,j tou pinaka P alliws bazei 0
		P = MatrixUtils.createRealMatrix(x, y);
		for (int i = 0; i < P.getRowDimension(); i++) {
			for (int j = 0; j < P.getColumnDimension(); j++) {
				if (R.getEntry(i, j) > 0) {
					P.setEntry(i, j, 1);
					// System.out.println("P " + P.getEntry(i, j));
				}
			}
		}
	}

	private static void setTableC() {
		C = MatrixUtils.createRealMatrix(x, y);
		for (int i = 0; i < C.getRowDimension(); i++) {
			for (int j = 0; j < C.getColumnDimension(); j++) {

				C.setEntry(i, j, 1 + a * R.getEntry(i, j));
				// System.out.println("C " + C.getEntry(i, j));

			}
		}
	}

	private static void setTableI(int k) {
		I = MatrixUtils.createRealMatrix(k, k);
		for (int i = 0; i < I.getRowDimension(); i++) {
			for (int j = 0; j < I.getColumnDimension(); j++) {
				if (i == j) {
					I.setEntry(i, j, 1);
				} else {
					I.setEntry(i, j, 0);
				}
				// System.out.println("I "+I.getEntry(i, j));

			}
		}
	}

	public static synchronized ArrayList<RealMatrix> distX() {
		if (fragment_counterX == 0) {
			int first = 0;
			int last = 0;
			for (int i = 0; i < fragments; i++) {
				first = last;// first is where the last one ended
				if (i == 0)// if length mod fragments != 0 o prwtos pairnei +1 stoixeio
				{
					first = 0;
					last = 0;
					if (X.getData().length % fragments != 0) {
						last = (X.getData().length / fragments) + 1;
					} else {
						last = X.getData().length / fragments;
					}
				} else {
					last = first + X.getData().length / fragments;
				}
				// System.out.println("First [" + first + "] last[" + last + "] - (" + i + ")
				// X");
				fragmentX_first[i] = first;
				fragmentX_last[i] = last;
			}
		}

		ArrayList<RealMatrix> list = new ArrayList();
		RealMatrix tmpF = MatrixUtils.createRealMatrix(1, 1);
		tmpF.setEntry(0, 0, fragmentX_first[fragment_counterX]);
		list.add(tmpF);// 0

		RealMatrix tmpL = MatrixUtils.createRealMatrix(1, 1);
		tmpL.setEntry(0, 0, fragmentX_last[fragment_counterX]);
		list.add(tmpL);// 1

		list.add(X);// 2
		list.add(Y);// 3

		list.add(P);// 4
		list.add(C);// 5
		list.add(I);// 6

		System.out.println("Sending fragmentX: " + fragment_counterX + " / " + fragments);
		// System.out.println("First [" + fragmentX_first[fragment_counterX] + "] last["
		// + fragmentX_last[fragment_counterX] + "] X");
		fragment_counterX++;
		if (fragment_counterX >= fragments) // ex megalutero tou 7
		{
			fragment_counterX = 0;
			mainApp.flag = false;
		}
		return list;
	}

	public static synchronized ArrayList<RealMatrix> distY() {
		if (fragment_counterY == 0) {
			int first = 0;
			int last = 0;
			for (int i = 0; i < fragments; i++) {
				first = last;// first is where the last one ended
				if (i == 0)// if length mod fragments != 0 o prwtos pairnei +1 stoixeio
				{
					first = 0;
					last = 0;
					if (Y.getData().length % fragments != 0) {
						last = (Y.getData().length / fragments) + 1;
					} else {
						last = Y.getData().length / fragments;
					}
				} else {
					last = first + Y.getData().length / fragments;
				}
				fragmentY_first[i] = first;
				fragmentY_last[i] = last;
			}

		}
		ArrayList<RealMatrix> list = new ArrayList();
		RealMatrix tmpF = MatrixUtils.createRealMatrix(1, 1);
		tmpF.setEntry(0, 0, fragmentY_first[fragment_counterY]);
		list.add(tmpF);// 0

		RealMatrix tmpL = MatrixUtils.createRealMatrix(1, 1);
		tmpL.setEntry(0, 0, fragmentY_last[fragment_counterY]);
		list.add(tmpL);// 1

		list.add(X);// 2
		list.add(Y);// 3

		list.add(P);// 4
		list.add(C);// 5
		list.add(I);// 6

		System.out.println("Sending fragmentY: " + fragment_counterY + " / " + fragments);
		// System.out.println("First [" + fragmentY_first[fragment_counterY] + "] last["
		// + fragmentY_last[fragment_counterY] + "] Y");
		fragment_counterY++;
		if (fragment_counterY >= fragments) // ex megalutero tou 7
		{
			fragment_counterY = 0;
			mainApp.flag = false;
		}
		return list;
	}

	// /*private static void fragmentP_X() {
	// int first = 0;
	// int last = 0;
	// for (int i = 0; i < fragments; i++) {
	// first = last;// first is where the last one ended
	// if (i == 0)// if length mod fragments != 0 o prwtos pairnei +1 stoixeio
	// {
	// first = 0;
	// last = 0;
	// if (P.getData().length % fragments != 0) {
	// last = (P.getData().length / fragments) + 1;
	// } else {
	// last = P.getData().length / fragments;
	// }
	// } else {
	// last = first + P.getData().length / fragments;
	// }
	// fragmentedP_X[i] = P.getSubMatrix(first, last, 0, P.getColumnDimension() -
	// 1);
	// }
	// }
	//
	// private static void fragmentP_Y() {
	//
	// int first = 0;
	// int last = 0;
	// for (int i = 0; i < fragments; i++) {
	// first = last;// first is where the last one ended
	// if (i == 0)// if length mod fragments != 0 o prwtos pairnei +1 stoixeio
	// {
	// first = 0;
	// last = 0;
	// if (P.getData().length % fragments != 0) {
	// last = (P.getData().length / fragments) + 1;
	// } else {
	// last = P.getData().length / fragments;
	// }
	// } else {
	// last = first + P.getData().length / fragments;
	// }
	// fragmentedP_Y[i] = P.getSubMatrix(first, last, 0, P.getRowDimension() - 1);
	// }
	// }
	//
	// private static void fragmentC_X() {
	// int first = 0;
	// int last = 0;
	// for (int i = 0; i < fragments; i++) {
	// first = last;// first is where the last one ended
	// if (i == 0)// if length mod fragments != 0 o prwtos pairnei +1 stoixeio
	// {
	// first = 0;
	// last = 0;
	// if (C.getData().length % fragments != 0) {
	// last = (C.getData().length / fragments) + 1;
	// } else {
	// last = C.getData().length / fragments;
	// }
	// } else {
	// last = first + C.getData().length / fragments;
	// }
	// fragmentedC_X[i] = C.getSubMatrix(first, last, 0, C.getColumnDimension() -
	// 1);
	// }
	// }
	//
	// private static void fragmentC_Y() {
	// int first = 0;
	// int last = 0;
	// for (int i = 0; i < fragments; i++) {
	// first = last;// first is where the last one ended
	// if (i == 0)// if length mod fragments != 0 o prwtos pairnei +1 stoixeio
	// {
	// first = 0;
	// last = 0;
	// if (C.getData().length % fragments != 0) {
	// last = (C.getData().length / fragments) + 1;
	// } else {
	// last = C.getData().length / fragments;
	// }
	// } else {
	// last = first + C.getData().length / fragments;
	// }
	// fragmentedC_Y[i] = C.getSubMatrix(first, last, 0, C.getRowDimension() - 1);
	// }
	// }

	public static synchronized double calculateError() {
		works_done++;
		double ret_error = 0;
		double error = 0;
		double norm = 0;
		for (int i = 0; i < X.getRowDimension(); i++) {
			for (int j = 0; j < X.getColumnDimension(); j++) {
				norm += Math.pow(X.getEntry(i, j), 2);
			}
		}
		for (int i = 0; i < Y.getRowDimension(); i++) {
			for (int j = 0; j < Y.getColumnDimension(); j++) {
				norm = norm + Math.pow(Y.getEntry(i, j), 2);
			}
		}
		norm = norm * mainApp.lamda;

		RealMatrix Xu = MatrixUtils.createRealMatrix(k, 1);
		RealMatrix Yi = MatrixUtils.createRealMatrix(k, 1);
		RealMatrix tmp = MatrixUtils.createRealMatrix(1, 1);
		for (int i = 0; i < R.getRowDimension(); i++) {
			for (int k = 0; k < X.getColumnDimension(); k++) {
				Xu.setEntry(k, 0, X.getEntry(i, k));
			}
			for (int j = 0; j < R.getColumnDimension(); j++) {
				for (int l = 0; l < Y.getColumnDimension(); l++) {
					Yi.setEntry(l, 0, Y.getEntry(i, l));
				}
				tmp = Xu.transpose().multiply(Yi);
				error = error + C.getEntry(i, j) * Math.pow(P.getEntry(i, j) - tmp.getEntry(0, 0), 2);
			}
		}
		ret_error = error + norm;

		return ret_error;
	}

	public static synchronized void updateX(int first, int last, RealMatrix x_tmp) {
		int count = 0;
		int x = 0;
		int y = 0;
		for (int i = first; i < last; i++) {
			for (int j = 0; j < x_tmp.getColumnDimension(); j++) {
				X.setEntry(i, j, x_tmp.getEntry(x, y));
				// System.out.println(i+" , "+j);
				y++;
			}
			y = 0;
			x++;
			count++;
		}
		x = 0;
		// System.out.println("X has been updated (" + count + ") lines.");

	}

	public static synchronized void updateY(int first, int last, RealMatrix y_tmp) {
		int count = 0;
		int x = 0;
		int y = 0;
		for (int i = first; i < last; i++) {
			for (int j = 0; j < y_tmp.getColumnDimension(); j++) {
				Y.setEntry(i, j, y_tmp.getEntry(x, y));
				// System.out.println(i+" , "+j);
				y++;
			}
			y = 0;
			x++;
			count++;
		}
		x = 0;
		// System.out.println("Y has been updated (" + count + ") lines.");

	}

}
