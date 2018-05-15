package nachos.threads;

import nachos.machine.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}

	/**
	 * Allocate a new lottery thread queue.
	 *
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer tickets from waiting
	 *            threads to the owning thread.
	 * @return a new lottery thread queue.
	 */

	/* Extension of PriorityScheduler functions */

	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new LotteryQueue(transferPriority);
	}

	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	// lottery thread state mimics threadstate above
	protected LotteryThreadState getLotteryThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		return (LotteryThreadState) thread.schedulingState;
	}

	// referenced priority scheduler
	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/// referenced priority scheduler
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);

		checkPriority(thread, priority);
	}

	// referenced priority scheduler
	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getLotteryThreadState(thread).numTickets;
	}

	public void checkPriority(KThread thread, int priority) {
		LotteryThreadState lotteryThread = getLotteryThreadState(thread);
		if (priority != lotteryThread.numTickets)
			lotteryThread.setPriority(priority);
	}

	// referenced priority scheduler
	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	// referenced priority scheduler w/ addition of effective tickets
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getLotteryThreadState(thread).effectiveTickets;
	}
	
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	// priorityMinimum will be starting from one in lotteryScheduler
	public static final int priorityMinimum = 1;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	// priorityMaximum will be the max value integer
	public static final int priorityMaximum = Integer.MAX_VALUE;

	protected class LotteryQueue extends ThreadQueue {

		boolean transferPriority;
		private LotteryThreadState resourceHolder;
		private int sumTickets;
		private HashSet<LotteryThreadState> waitQueue = new HashSet<LotteryThreadState>();

		LotteryQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {

			// Disable interrupt
			Lib.assertTrue(Machine.interrupt().disabled());

			// typecast thread
			LotteryThreadState lts = getLotteryThreadState(thread);

			// thread is waiting for access
			lts.waitForAccess(this);

		}

		public void acquire(KThread thread) {

			// Disable interrupt
			Lib.assertTrue(Machine.interrupt().disabled());

			// typecast thread
			LotteryThreadState lts = getLotteryThreadState(thread);

			// acquire thread
			lts.acquire(this);
		}

		public void updateEP() {
			int updateTickets = 0;

			Iterator<LotteryThreadState> nextLottery = waitQueue.iterator();
			while (nextLottery.hasNext()) {
				LotteryThreadState ts = nextLottery.next();
				updateTickets += ts.effectiveTickets;
				sumTickets = updateTickets;
			}
		}

		// remove waiting thread
		public void noWait(LotteryThreadState lotteryThread) {
			// if thread is removed from wait queue
			if (waitQueue.remove(lotteryThread)) {
				// update the new effective & resource holder
				updateEP();
				updateResourceHolder();
			}
		}

		public KThread nextThread() {

			// Disable interrupt!
			Lib.assertTrue(Machine.interrupt().disabled());

			// check for non-empty wait queue
			if (!waitQueue.isEmpty()) {
				// return the thread pickNextThread returns
				return pickNextThread();
			} else {
				// none found? return null
				return null;
			}
		}

		public KThread pickNextThread() {

			/*
			 * Slight difference from priority scheduler; instead of choosing a thread with
			 * the highest priority, pick a thread based on how a lottery system (random
			 * ticket) works.
			 */

			// lottery thread to be returned
			KThread lotteryThread = null;

			// random number generator
			Random randomGen = new Random();

			// generate a random ticket
			int randomTicket = randomGen.nextInt(sumTickets) + 1;

			// iterate through lottery thread queue
			Iterator<LotteryThreadState> nextLottery = waitQueue.iterator();
			// while thread exists
			while (nextLottery.hasNext()) {
				// keep on updating the random ticket
				LotteryThreadState ts = nextLottery.next();
				int lotteryTicket = ts.effectiveTickets;
				randomTicket = randomTicket - lotteryTicket;

				// once it reaches negative
				if (randomTicket <= 0) {
					// acquire the last known lottery thread
					lotteryThread = ts.thread;
					ts.acquire(this);
					// terminate loop
					break;
				}
			}
			// return final lottery thread
			return lotteryThread;
		}

		public void print() {
			// Skip
		}

		// update resource holder
		public void updateResourceHolder() {
			// if not null, update it
			if (resourceHolder != null) {
				resourceHolder.updateEP2();
			}
		}
	}

	/**
	 * The scheduling state of a thread. This should include the thread's priority,
	 * its effective priority, any objects it owns, and the queue it's waiting for,
	 * if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */

	protected class LotteryThreadState {

		//kthread variable
		KThread thread;
		//set number of tickets & effective tickets as min temporarily
		private int numTickets = priorityMinimum;
		private int effectiveTickets = priorityMinimum;
		//HashSet of type LotteryQueue for resource and wait queue
		private HashSet<LotteryQueue> resourceQueue = new HashSet<LotteryQueue>();
		private HashSet<LotteryQueue> waitQueue = new HashSet<LotteryQueue>();

		LotteryThreadState(KThread thread) {
			this.thread = thread;
		}

		//set current priority (tickets in this case)
		public void setPriority(int priority) {
			this.numTickets = priority;
			updateEP2();
		}
		
		//release lottery queue
		public void release(LotteryQueue lq) {
			//if resource queue hasn't removed lottery queue and it's current holder
			if (!resourceQueue.remove(lq) && lq.resourceHolder == this) {
				//update them
				updateHolder(lq);
				updateResourceQueue();
				updateWaitQueue();
			}
		}

		public void updateResourceQueue() {
			//ticket to be returned
			int Tickets = numTickets;

			// iterate through the resource queue
			Iterator<LotteryQueue> nextLottery = resourceQueue.iterator();
			// while queue exists
			while (nextLottery.hasNext()) {
				LotteryQueue lotteryQueue = nextLottery.next();

				// check for transfer priority
				if (lotteryQueue.transferPriority) {

					// loop through threadstate of wait queue
					for (LotteryThreadState lotteryThread : lotteryQueue.waitQueue)
						// sums up lottery thread state tickets
						Tickets += lotteryThread.effectiveTickets;
				}
				// return sum tickets and update effective tickets
				effectiveTickets = Tickets;
			}
		}

		public void updateWaitQueue() {

			// iterate through the Lottery wait Queue
			Iterator<LotteryQueue> nextLottery = waitQueue.iterator();
			// while queue exists
			while (nextLottery.hasNext()) {
				LotteryQueue lotteryQueue = nextLottery.next();
				lotteryQueue.updateEP();
				if (lotteryQueue.transferPriority && lotteryQueue.resourceHolder != null)
					lotteryQueue.resourceHolder.updateEP2();
			}
		}
		
		//update resource holder
		public void updateHolder(LotteryQueue lq) {
			if (!resourceQueue.remove(lq)) {
				lq.resourceHolder = null;
			}
		}
		
		//update resource and wait queue
		public void updateQueue(LotteryQueue lq) {
			resourceQueue.add(lq);
			waitQueue.remove(lq);
		}

		//update effective priority for lottery queue
		private void updateEP2() {
			updateResourceQueue();
			updateWaitQueue();
		}

		//if exists, clean previous resource queue holder
		public void wipePrevious(LotteryQueue lq) {
			if (lq.resourceHolder != null) {
				lq.resourceHolder.release(lq);
			}
		}

		//no longer waiting; update
		public void removeWait(LotteryQueue lq) {
			lq.noWait(this);
			lq.resourceHolder = this;
			updateQueue(lq);
			updateEP2();
		}

		public void acquire(LotteryQueue lq) {
			if (lq.resourceHolder != this) {
				wipePrevious(lq);
			}
			removeWait(lq);
		}

		//priority donation
		public void donation(LotteryQueue lq) {

			boolean check = lq.waitQueue.contains(this);
			if (!check) {
				waitQueue.add(lq);
				lq.waitQueue.add(this);

				//if priority donation
				if (lq.transferPriority && lq.resourceHolder != null) {
					//update resource holder with lottery thread state
					lq.resourceHolder.updateEP2();
				} else {
					//else update lottery queue
					lq.updateEP();
				}
			}
		}

		public void waitForAccess(LotteryQueue lq) {
			// if current resource owner & queue hasn't been removed
			if(lq.resourceHolder == this) {
				//update resourceHolder
				updateHolder(lq);
				// update both queue
				updateResourceQueue();
				updateWaitQueue();
			}
			//priority donation
			donation(lq);
		}
	}
}