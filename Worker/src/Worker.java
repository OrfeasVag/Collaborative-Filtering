import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

public class Worker {
	Socket connection;
	ObjectInputStream in = null;
	ObjectOutputStream out = null;
	static RealMatrix matrix;
	int k, z, worker_id, first, last;
	int type;// if type = 0 x , if type = 1 y
	double[][] table;
	static RealMatrix X, X_return;
	static RealMatrix Y, Y_return;
	double lamda;
	static RealMatrix I;
	static RealMatrix C;
	static RealMatrix P;
	static RealMatrix Cu, Pu, Ci, Pi;
	ArrayList<RealMatrix> list;
	int num;

	public Worker() {
		connection = null;
	}

	public Worker(Socket connection) {
		this.connection = connection;
	}

	@SuppressWarnings("unchecked")
	public void initialize() {// main
		int cores = Runtime.getRuntime().availableProcessors();
		long freemem = Runtime.getRuntime().freeMemory();
		System.out.println("Cores: " + cores + " , Memory: " + freemem + " .");
		try {
			out = new ObjectOutputStream(connection.getOutputStream());
			in = new ObjectInputStream(connection.getInputStream());
			// out.flush();// ka8e fora afou grapsw kanw flush()
			// ===========================================
			// TYPE
			// 0 is worker
			// 1 is client
			out.writeInt(0);
			out.flush();
			// ===========================================
			worker_id = in.readInt();

			out.writeInt(cores);
			out.flush();
			out.writeLong(freemem);
			out.flush();
			while (in.readInt() == 1) { // start ?
				System.out.println("Hi i am worker with id: " + worker_id + ".");
				Y = null;
				X = null;
				P = null;
				C = null;
				I = null;
				lamda = in.readDouble();
				list = (ArrayList<RealMatrix>) in.readObject();

				first = (int) list.get(0).getEntry(0, 0);
				last = (int) list.get(1).getEntry(0, 0);

				X = list.get(2);
				Y = list.get(3);
				P = list.get(4);
				C = list.get(5);
				I = list.get(6);

				// System.out.println("X rows " + X.getRowDimension() + " , columns " +
				// X.getColumnDimension());
				// System.out.println("Y rows " + Y.getRowDimension() + " , columns " +
				// Y.getColumnDimension());
				// System.out.println("P rows " + P.getRowDimension() + " , columns " +
				// P.getColumnDimension());
				// System.out.println("C rows " + C.getRowDimension() + " , columns " +
				// C.getColumnDimension());
				// System.out.println("I rows " + I.getRowDimension() + " , columns " +
				// I.getColumnDimension());

				int count = 0;
				// System.out.println("Calculating y_i");
				Y_return = MatrixUtils.createRealMatrix(last - first, Y.getColumnDimension());
				for (int i = 0; i < last - first; i++) {
					// System.out.println(count + "/ " + Y.getRowDimension() + " y_i");
					// System.out.println("working Line " + count + " Yi");
					calculate_Yi(i);
					// System.out.println("done Line " + count + " Yi");
					count++;
				}
				// System.out.println("Done calculating y_i lines: " + count);

				out.writeObject(Y_return);
				out.flush();

				out.writeInt(first);
				out.flush();

				out.writeInt(last);
				out.flush();

				//System.out.println("Done with yi");
				Y = null;
				X = null;
				P = null;
				C = null;
				I = null;

				list = (ArrayList<RealMatrix>) in.readObject();

				first = (int) list.get(0).getEntry(0, 0);
				last = (int) list.get(1).getEntry(0, 0);

				X = list.get(2);
				Y = list.get(3);
				P = list.get(4);
				C = list.get(5);
				I = list.get(6);
				// System.out.println("Tables for X ok");
				// System.out.println("X rows " + X.getRowDimension() + " , columns " +
				// X.getColumnDimension());
				// System.out.println("Y rows " + Y.getRowDimension() + " , columns " +
				// Y.getColumnDimension());
				// System.out.println("P rows " + P.getRowDimension() + " , columns " +
				// P.getColumnDimension());
				// System.out.println("C rows " + C.getRowDimension() + " , columns " +
				// C.getColumnDimension());
				// System.out.println("I rows " + I.getRowDimension() + " , columns " +
				// I.getColumnDimension());

				// System.out.println("Calculating x_u");
				X_return = MatrixUtils.createRealMatrix(last - first, X.getColumnDimension());
				count = 0;
				for (int i = 0; i < last - first; i++) {

					// System.out.println(count + "/ " + X.getRowDimension() + " x_u");
					// System.out.println("working Line " + count + " Xu");

					calculate_Xu(i);
					// System.out.println("done Line " + count + " Xu");
					count++;
				}
				// System.out.println("Done calculating x_u lines: " + count);

				out.writeObject(X_return);
				out.flush();

				out.writeInt(first);
				out.flush();

				out.writeInt(last);
				out.flush();
				System.out.println("Done X,Y");
			}
			System.out.println("Closing connection [" + worker_id + "]");
		} catch (Exception e) {
			System.out.println("Exception Worker.");
			System.out.println(e.getMessage());

		}

	}

	public void calculateCu(int u) {
		Cu = MatrixUtils.createRealMatrix(C.getColumnDimension(), C.getColumnDimension());
		for (int i = 0; i < C.getColumnDimension(); i++) {
			// System.out.println("Cu " + C.getEntry(u, i));
			Cu.setEntry(i, i, C.getEntry(u, i));
		}
		// System.out.println("CU OK");
	}

	public void calculatePu(int u) {
		Pu = MatrixUtils.createRealMatrix(C.getColumnDimension(), 1);
		for (int i = 0; i < P.getColumnDimension(); i++) {
			// System.out.println("Pu " + P.getEntry(u, i));
			Pu.setEntry(i, 0, P.getEntry(u, i));
		}
		// System.out.println("PU OK");
	}

	public void calculateCi(int i) {
		Ci = MatrixUtils.createRealMatrix(C.getRowDimension(), C.getRowDimension());
		for (int j = 0; j < C.getRowDimension(); j++) {
			// System.out.println("Ci " + C.getEntry(j, i));
			Ci.setEntry(j, j, C.getEntry(j, i));
		}
		// System.out.println("CI OK");

	}

	public void calculatePi(int i) {
		Pi = MatrixUtils.createRealMatrix(C.getRowDimension(), 1);
		for (int j = 0; j < P.getRowDimension(); j++) {
			// System.out.println("Pi " + P.getEntry(j, i));
			Pi.setEntry(j, 0, P.getEntry(j, i));
		}
		// System.out.println("PI OK");
	}

	public RealMatrix calculateYY() {
		RealMatrix tmp = MatrixUtils.createRealMatrix(Y.getColumnDimension(), Y.getColumnDimension());
		tmp = (Y.transpose()).multiply(Y);

		return tmp;
	}

	public RealMatrix calculateXX() {

		RealMatrix tmp = MatrixUtils.createRealMatrix(X.getRowDimension(), X.getRowDimension());
		tmp = (X.transpose()).multiply(X);

		return tmp;
	}

	public void calculate_Xu(int u) {
		calculatePu(u);
		calculateCu(u); // ftiaxnw ta pu , u me bash t u
		// System.out.println("hey1");
		// diagwnios tmp1 = cu (diagwn) -1
		RealMatrix tmp1 = MatrixUtils.createRealMatrix(Cu.getRowDimension(), Cu.getRowDimension());

		for (int i = 0; i < Cu.getRowDimension(); i++) {
			for (int j = 0; j < Cu.getColumnDimension(); j++) {
				if (i == j) {
					tmp1.setEntry(i, j, Cu.getEntry(i, j) - 1);
				}
			}
		}
		// System.out.println("hey2");
		RealMatrix tmp2 = MatrixUtils.createRealMatrix(I.getRowDimension(), I.getRowDimension());
		// System.out.println("hey3");
		for (int i = 0; i < I.getRowDimension(); i++) {
			for (int j = 0; j < I.getColumnDimension(); j++) {
				if (i == j) {
					tmp2.setEntry(i, j, lamda);
				}
			}
		} // tmp2 = ë*I(diag)
			// System.out.println("hey4");
		/*
		 * System.out.println(" =="); System.out.println("calculateYY() rows " +
		 * calculateYY().getRowDimension() + " , columns " +
		 * calculateYY().getColumnDimension()); System.out.println("Y.transpose() rows "
		 * + Y.transpose().getRowDimension() + " , columns " +
		 * Y.transpose().getColumnDimension()); System.out.println("tmp1 rows " +
		 * tmp1.getRowDimension() + " , columns " + tmp1.getColumnDimension());
		 * System.out.println("Y rows " + Y.getRowDimension() + " , columns " +
		 * Y.getColumnDimension()); System.out.println("tmp2 rows " +
		 * tmp2.getRowDimension() + " , columns " + tmp2.getColumnDimension());
		 */
		// System.out.println("hey5");
		RealMatrix tmp3 = new QRDecomposition(calculateYY().add(Y.transpose().multiply(tmp1).multiply(Y)).add(tmp2))
				.getSolver().getInverse(); // yT*y + (yT * cU * Y + ë*I)-1 //san to lab
		// System.out.println("hey6");
		RealMatrix Xu = tmp3.multiply(Y.transpose()).multiply(Cu).multiply(Pu);// xu
																				// =
																				// yT*y
																				// +
																				// (yT
																				// *
																				// cU
																				// *
																				// Y
																				// +
																				// ë*I)-1
																				// *
																				// yT
																				// *
																				// cU
																				// *
																				// pU
		// bazw tis times ston X
		// System.out.println("hey7");
		for (int i = 0; i < Xu.getRowDimension(); i++) {
			// System.out.println(Xu.getEntry(i, 0));
			X_return.setEntry(u, i, Xu.getEntry(i, 0));
		}

	}

	public void calculate_Yi(int i) {
		// System.out.println("hi1");
		calculatePi(i);
		calculateCi(i); // ftiaxnw ta pi , ci me bash t i
		RealMatrix tmp1 = MatrixUtils.createRealMatrix(Ci.getRowDimension(), Ci.getRowDimension());
		// System.out.println("hi2");

		for (int z = 0; z < Ci.getRowDimension(); z++) {
			for (int j = 0; j < Ci.getColumnDimension(); j++) {
				if (z == j) {
					tmp1.setEntry(z, j, Ci.getEntry(z, j) - 1);
				}
			}
		} // diagwnios tmp1 = ci (diagwn) -1
			// System.out.println("hi3");

		RealMatrix tmp2 = MatrixUtils.createRealMatrix(I.getRowDimension(), I.getRowDimension());
		for (int z = 0; z < I.getRowDimension(); z++) {
			for (int j = 0; j < I.getColumnDimension(); j++) {
				if (z == j) {
					tmp2.setEntry(z, j, lamda);
				}
			}
		} // tmp2 = ë*I(diag)
			// System.out.println("hi4");

		/*
		 * System.out.println(" =="); System.out.println("calculateXX() rows " +
		 * calculateXX().getRowDimension() + " , columns " +
		 * calculateXX().getColumnDimension()); System.out.println("X.transpose() rows "
		 * + X.transpose().getRowDimension() + " , columns " +
		 * X.transpose().getColumnDimension()); System.out.println("tmp1 rows " +
		 * tmp1.getRowDimension() + " , columns " + tmp1.getColumnDimension());
		 * System.out.println("X rows " + X.getRowDimension() + " , columns " +
		 * X.getColumnDimension()); System.out.println("tmp2 rows " +
		 * tmp2.getRowDimension() + " , columns " + tmp2.getColumnDimension());
		 */

		RealMatrix tmp3 = new QRDecomposition(calculateXX().add(X.transpose().multiply(tmp1).multiply(X)).add(tmp2))
				.getSolver().getInverse(); // xT*X + (xT * ci * X * ë*I)-1
		// System.out.println("hi5");

		RealMatrix Yi = tmp3.multiply(X.transpose()).multiply(Ci).multiply(Pi);
		// System.out.println("hi6");
		// xT*X
		// +
		// (xT
		// *
		// ci
		// *
		// X
		// *
		// ë*I)-1
		// *xT
		// *
		// ci
		// *
		// pi
		for (int z = 0; z < Yi.getRowDimension(); z++) {
			// System.out.println(Yi.getEntry(i, 0));
			Y_return.setEntry(i, z, Yi.getEntry(z, 0));
		}

	}

	public String toString() {
		return "This is a Master object\n" + "Socket: " + this.connection + " .";

	}

}
