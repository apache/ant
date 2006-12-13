public class Beta extends Alpha {
    public String toString() {
	return "beta " + super.toString();
    }
    public static void main(String [] args) {
	Beta myBeta = new Beta();
	System.out.println(myBeta.toString());
    }
}
