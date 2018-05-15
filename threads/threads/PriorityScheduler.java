package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from waiting
	 *            threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

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

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {

		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		private void cache() {

			// check for priority donation
			if (this.resourceHolder != null && transferPriority) {
				resourceHolder.cache();
			}

			this.priorityCheck = true;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState threads = getThreadState(thread);
			this.waitingQueue.add(threads);
			threads.waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState threads = getThreadState(thread);
			if (this.resourceHolder != null) {
				this.resourceHolder.release(this);
			}
			this.resourceHolder = threads;
			threads.acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			// implement me

			// if waitingQueue isn't empty
			if (!waitingQueue.isEmpty()) {
				ThreadState nextThread = pickNextThread();
				// make sure the thread pickNextThread returns isn't null
				if (nextThread != null) {
					// if it isn't, acquire that thread
					acquire(nextThread.thread);
					// then remove the thread from queue
					waitingQueue.remove(nextThread);
				}
				// return next thread
				return nextThread.thread;
			}

			// if transfer priority and resource holder exists
			if (transferPriority && resourceHolder != null) {
				// remove current resource holder from queue
				this.resourceHolder.resourceQueue.remove(this);
			}

			// everything fails, return null
			return null;

		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return, without
		 * modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */

		protected ThreadState pickNextThread() {

			// pick the next thread from last element of queue
			ThreadState pickThread = waitingQueue.pollLast();

			// determine thread existence
			if (pickThread != null) {
				// return the thread picked
				return (pickThread);
				// if none found, return null
			} else {
				return null;
			}

		}

		// getEffectivePriority for PriorityQueue
		public int getEffectivePriority() {

			if (this.transferPriority && this.priorityCheck) {
				this.ePriority = priorityMinimum;
				// iterate through threads (type KThread) in wait queue
				Iterator<ThreadState> nextT = waitingQueue.iterator();
				while (nextT.hasNext()) {
					// compare current effective's with next thread effective priority
					ePriority = Math.max(ePriority, (nextT.next()).getEffectivePriority());
				}
				priorityCheck = false;
			}
			// obtain max effective priority
			return ePriority;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting threads to
		 * the owning thread.
		 */

		public boolean transferPriority;
		// ThreadState waiting queue list
		protected LinkedList<ThreadState> waitingQueue = new LinkedList<ThreadState>();
		// threadstate resource holder
		private ThreadState resourceHolder = null;
		// threadstate effective priority; temp min
		private int ePriority = priorityMinimum;
		// variable checks for priority change
		boolean priorityCheck = false;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's priority,
	 * its effective priority, any objects it owns, and the queue it's waiting for,
	 * if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		// resource holding queue
		protected LinkedList<PriorityQueue> resourceQueue = new LinkedList<PriorityQueue>();
		// resource waiting Queue
		protected LinkedList<PriorityQueue> waitingQueue = new LinkedList<PriorityQueue>();
		// effective priority of the associated thread; temp min
		int effectivePriority = priorityMinimum;
		// wait time for threads comparison
		protected long waitTime = Machine.timer().getTime();
		// check for priority changes
		boolean priorityCheck = false;

		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread
		 *            the thread this state belongs to.
		 */

		public ThreadState(KThread thread) {
			this.thread = thread;

			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me

			if (!resourceQueue.isEmpty() && this.priorityCheck) {
				this.effectivePriority = this.getPriority();
				// iterate through threads in resourceQueue
				Iterator<PriorityQueue> nextThread = resourceQueue.iterator();
				while (nextThread.hasNext()) {
					/*
					 * compares current priority to the effective priority in priority queue and
					 * determine maximum effective priority among the threads
					 */
					effectivePriority = Math.max(effectivePriority, nextThread.next().getEffectivePriority());
				}
				priorityCheck = false;
			}
			// return final effective priority
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {

			// implement me

			if (this.priority == priority)
				return;

			this.priority = priority;

			Iterator<PriorityQueue> thread = waitingQueue.iterator();
			while (thread.hasNext()) {

				thread.next().cache();
			}
		}

		public void release(PriorityQueue waitQueue) {

			// remove queue from resource queue
			this.resourceQueue.remove(waitQueue);

			// cache
			cachePriority();
		}

		/*
		 * calculate maximum effective priority based on PriorityQueue class to compare
		 * w/ the ThreadState's effective priority
		 */

		public int calcMaxPriority() {

			// current priority
			int calcMaxEP = this.priority;

			// iterate through the Priority queue
			Iterator<PriorityQueue> thread = resourceQueue.iterator();
			while (thread.hasNext()) {

				// determine maximum priority by comparing w/ effective priority
				calcMaxEP = Math.max(calcMaxEP, (thread.next()).getEffectivePriority());

			} // return max priority from PriorityQueue
			return calcMaxEP;
		}

		// update effective priority
		public void updateEP() {

			// set current effective priority as the new max priority
			effectivePriority = calcMaxPriority();

			// retrieve last thread of resource queue
			PriorityQueue thread = resourceQueue.pollLast();

			// if they have the same effective priority
			if (effectivePriority == calcMaxPriority()) {
				// iterate through all threads in priority queue
				Iterator<PriorityQueue> threads = resourceQueue.iterator();
				while (threads.hasNext()) {

					int threadEP = thread.getEffectivePriority();
					int nextThreadEP = threads.next().getEffectivePriority();

					// if same priority
					if (threadEP == nextThreadEP) {
						// compare wait time
						/*
						 * if (thread.waitTime <= threads.next().waitTime) { effectivePriority =
						 * nextThreadEP; }
						 */
					}
				}
			}
		}

		private void cache() {
			if (this.priorityCheck)
				return;
			this.priorityCheck = true;

			Iterator<PriorityQueue> thread = waitingQueue.iterator();
			while (thread.hasNext()) {

				thread.next().cache();
			}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is the
		 * associated thread) is invoked on the specified priority queue. The associated
		 * thread is therefore waiting for access to the resource guarded by
		 * <tt>waitQueue</tt>. This method is only called if the associated thread
		 * cannot immediately obtain access.
		 *
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 *
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {

			// implement me

			// add thread to wait queue
			waitQueue.waitingQueue.add(this);

			// remove thread from resource queue
			this.resourceQueue.remove(waitQueue);

			waitQueue.cache();

		}

		public void cachePriority() {
			this.cache();
		}

		/**
		 * Called when the associated thread has acquired access to whatever is guarded
		 * by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me

			// acquired thread to resource queue
			this.resourceQueue.add(waitQueue);

			// thread is no longer waiting, remove it!
			waitQueue.waitingQueue.remove(this);

			cachePriority();
		}
	}
}