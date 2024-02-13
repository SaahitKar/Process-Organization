import java.util.*;
import java.io.File;

class Sim {
	public static boolean debugOn = false;

	public interface Scheduler {
        public void initialize(Sim sim);
        public void timeout(Sim sim);
        public void stopRunning(Sim sim);
        public void arrive(Process p,Sim sim);
        public void unblock(Process p,Sim sim);
        public void idle(Sim sim);
	}

    public static void debug(Object msg) {
		if(debugOn) System.out.println(msg);
	}

    public static class Process {
        private int pid;
        private int arrive;
        private ArrayList<Integer> activities;
    	private Stats stats;
    
    	public Process(int pid, int arrive, ArrayList<Integer> activities) {
            this.pid = pid;
            this.arrive = arrive;
            this.activities = activities;
    	}
    
    	public int getPID() { return pid; }
    	public int getArrive() { return arrive; }
    	public ArrayList<Integer> getActivities() { return activities; }
    	public Stats getStats() { return stats; }
    	public void setStats(Stats stats) { this.stats = stats; }
    
    	public String toString() {
            return "Proccess " + pid + ", Arrive " + arrive + ": " + Arrays.toString(activities.toArray());
    	}
    }

	enum EventType { ARRIVAL, UNBLOCK }
	class Event implements Comparable<Event> {
		private EventType type;
		private Process process;
		private int timeStamp;
		Event(EventType t, Process p, int time) {
			type = t;
			process = p;
			timeStamp = time;
		}

		EventType getType() {return type;}
		Process getProcess() {return process;}
		int getTimeStamp() {return timeStamp;}
        
		public int compareTo(Event b)
        {
            if(getTimeStamp() == b.getTimeStamp()) {
                // Break Tie with event getType()
                if(getType() == b.getType()){
                    // Break getType() tie by pid
                    return getProcess().getPID() - b.getProcess().getPID();
				}
                else return getType().compareTo(b.getType());
			}
            else return getTimeStamp() - b.getTimeStamp();
        }
	}

    public static class InvalidFileException extends Exception {
    
      public InvalidFileException(String message){
         super(message);
      }
    
    }

    static ArrayList<Process> parseProcessFile(String procFile) throws InvalidFileException {
		int lineNumber = 1;
        ArrayList<Process> procs = new ArrayList<Process>();
		try{
			Scanner FileIn = new Scanner(new File(procFile));
			int pid = 0;
			while(FileIn.hasNextLine()) {
				int arrive;
				String[] actStr;
				ArrayList<Integer> act;
				arrive = FileIn.nextInt();
				String tmp  = FileIn.nextLine().strip();
				actStr = tmp.split("\\s+");
                if(actStr.length == 0) {
                    throw new InvalidFileException("Process missing activities and possible the arrival time at line " + lineNumber);
				}
                // Check to make sure there is a final CPU activity
                // We assume the first activity is CPU, and it alternates from there.
                // This means there must be an odd number of activities or an even number
                // of ints on the line (the first being arrival time)
                if(actStr.length % 2 != 1)
                    throw new InvalidFileException("Process with no final CPU activity at line " + lineNumber);
				act = new ArrayList<Integer>();
				for(int i = 0; i<actStr.length; i++) {
					act.add(Integer.parseInt(actStr[i]));
				}
		        procs.add(new Process(pid,arrive,act));
				pid++;
				lineNumber++;
		  }
		} catch(Exception e) {
			throw new InvalidFileException("At line " + lineNumber + ": " + e.toString());
		}

		return procs;
     }

	public static class SchedulerInfo {
		public String name;
		public HashMap<String,String> parameters;
	}

    public static SchedulerInfo parseSchedulerFile(String file) throws InvalidFileException {
		SchedulerInfo info = new SchedulerInfo();
		info.parameters = new HashMap<String,String>();
		int lineNumber = 1;
		try {
			Scanner FileIn = new Scanner(new File(file));
            info.name = FileIn.nextLine();
			while(FileIn.hasNextLine()) {
				String line = FileIn.nextLine().strip();
                String[] split = line.split("\\s*=\\s*");
				if(split.length != 2) throw new InvalidFileException("Invalid Scheduler option on line " + lineNumber);
				info.parameters.put(split[0],split[1]);
                lineNumber = lineNumber + 1;
			}
		} catch(Throwable t) {
			throw new InvalidFileException("Scheduler File, line " + lineNumber + ": " + t.getMessage());
		}
        return info;
	}

    // Instance Information	
	private Scheduler sched;
	private Integer timer, runningTime;
	private int clock;
	private PriorityQueue<Event> events;

	public <T extends Scheduler> Sim(T sched) {
		this.sched = sched;
        events = new PriorityQueue<Event>();
        clock = 0;
	}

	public Integer getTimer() {return timer;}
	public Integer getRunningTime() {return runningTime;}
	public int getClock() {return clock;}
	public void setRunningTime(Integer t) {runningTime = t;}
	public void setClock(Integer t) {clock = t;}

	public void run() {
        sched.initialize(this);
        Integer move = getTimeForward();
        while(move != null) {
            if(handleTimeDone(move)){
                move = getTimeForward();
                while(move != null && handleTimeDone(move))
                    move = getTimeForward();
			}
            else{
                if(timer != null) timer -= move;
                if(runningTime != null) runningTime -= move;
                clock = events.peek().getTimeStamp();
                processEvent(events.poll());
			}
            while(events.size() > 0 && events.peek().getTimeStamp() == clock)
                processEvent(events.poll());
            if(runningTime == null) sched.idle(this);
            move = getTimeForward();
		}
	}

    private Integer getTimeForward() {
        if(events.size() > 0) 
            return events.peek().getTimeStamp() - clock;
		else if(runningTime != null)
            return runningTime;
        else
            return timer;
	}

    private boolean handleTimeDone(int move) {
        boolean canTimer = timer != null && timer <= move;
        boolean canStopRunning = runningTime != null && runningTime <= move;
        if(canTimer && (!canStopRunning || timer < runningTime)) {
           clock += timer;
           if(canStopRunning) runningTime -= timer;
           timer = null;
           sched.timeout(this);
           return true;
		}
		else if(canStopRunning) {
           clock += runningTime;
           if(canTimer) timer -= runningTime;
           runningTime = null;
           sched.stopRunning(this);
           return true;
		}
        return false;
	}

    private void processEvent(Event e) {
        if(e.getType() == EventType.ARRIVAL) sched.arrive(e.process, this);
        else sched.unblock(e.process, this); //e.getType() == EventType.UNBLOCK
	}

    public void addArrival(Process p){
        events.add(new Event(EventType.ARRIVAL, p, p.getArrive()));
	}

    public void addUnblockEvent(Process p,int t) {
        events.add(new Event(EventType.UNBLOCK, p, clock+t));
	}

}
