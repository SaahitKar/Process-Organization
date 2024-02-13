import java.util.ArrayList;

class Project3 {

	// Template Scheduler Class for FCFS
	static class FCFSSchedueler implements Sim.Scheduler {
		ArrayList<Sim.Process> procs;

		public FCFSSchedueler(ArrayList<Sim.Process> procs) {
			this.procs = procs;
		}
        public void initialize(Sim sim) {
		}
        public void timeout(Sim sim) {
		}
        public void stopRunning(Sim sim) {
		}
        public void arrive(Sim.Process p,Sim sim){
		}
        public void unblock(Sim.Process p,Sim sim) {
		}
        public void idle(Sim sim) {
		}
	}
	public static void main(String[] args) {
		try {

			//Driver code here

		} //catch(Sim.InvalidFileException e) {
		catch(Exception e) {
			System.out.println(e);
		}
	}
}
