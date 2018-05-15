package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Lib;
import nachos.threads.Boat.Hawian.Type;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest() {
		BoatGrader b = new BoatGrader();
		
//		System.out.println("\n ***Testing Boats with only 2 children***");
//		begin(0, 2, b);
//		testOahu(0, 2);
//		System.out.println("\n ***Testing Boats with only 2 children*** - TEST PASSED");
//		
//		System.out.println("\n ***Testing Boats with only 5 children***");
//		begin(0, 5, b);
//		testOahu(0, 5);
//		System.out.println("\n ***Testing Boats with only 5 children*** - TEST PASSED");
//		
//		System.out.println("\n ***Testing Boats with only 2 children, 1 adult***");
//		begin(1, 2, b);
//		testOahu(1, 2);
//		System.out.println("\n ***Testing Boats with only 2 children, 1 adult*** - TEST PASSED");
//		
//		System.out.println("\n ***Testing Boats with only 2 children, 2 adult***");
//		begin(2, 2, b);
//		testOahu(2, 2);
//		System.out.println("\n ***Testing Boats with only 2 children, 2 adult*** - TEST PASSED");
//
//		System.out.println("\n ***Testing Boats with only 2 children, 3 adult***");
//		begin(3, 2, b);
//		testOahu(3, 2);
//		System.out.println("\n ***Testing Boats with only 2 children, 3 adult*** - TEST PASSED");
//		
//		System.out.println("\n ***Testing Boats with only 3 children, 2 adult***");
//		begin(2, 3, b);
//		testOahu(2, 3);
//		System.out.println("\n ***Testing Boats with only 3 children, 2 adult*** - TEST PASSED");
//		
//		System.out.println("\n ***Testing Boats with only 3 children, 3 adult***");
//		begin(3, 3, b);
//		testOahu(3, 3);
//		System.out.println("\n ***Testing Boats with only 3 children, 3 adult*** - TEST PASSED");

		System.out.println("\n ***Testing Boats with only 13 children, 3 adult***");
		begin(3, 13, b);
		testOahu(3, 13);
		System.out.println("\n ***Testing Boats with only 13 children, 3 adult*** - TEST PASSED");

		System.out.println("\n ***Testing Boats with only 3 children, 13 adult***");
		begin(13, 3, b);
		testOahu(13, 3);
		System.out.println("\n ***Testing Boats with only 3 children, 13 adult*** - TEST PASSED");
		
		System.out.println("\n ***Testing Boats with only 13 children, 13 adult***");
		begin(13, 13, b);
		testOahu(13, 13);
		System.out.println("\n ***Testing Boats with only 13 children, 13 adult*** - TEST PASSED");
		
		System.out.println("\n ***Testing Boats with only 100 children, 100 adult***");
		begin(100, 100, b);
		testOahu(100, 100);
		System.out.println("\n ***Testing Boats with only 100 children, 100 adult*** - TEST PASSED");
	
	//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
	//  	begin(1, 2, b);
	
	//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
	//  	begin(3, 3, b);
    }
    
    public static void testOahu(int numAdults, int numKids) {
    		Lib.assertTrue(Hawian.adults.length == numAdults, "did not create the correct number adults, expected: " + numAdults + " actual: " + Hawian.adults.length);

    		Lib.assertTrue(Hawian.kids.length == numKids, "did not create the correct number kids, expected: " + numKids + " actual: " + Hawian.kids.length);
    		
    		Lib.assertTrue(Hawian.captain.type == Type.Child, "capitan should always be a child");
    	
		Lib.assertTrue(Hawian.numAdultsOnOahu == 0, "left adults on Oahu: " + Hawian.numAdultsOnOahu);
		
		int kidsLeftBehind = Hawian.numPeopleOnOahu - Hawian.numAdultsOnOahu;
		Lib.assertTrue( kidsLeftBehind == 0, "left kids on Oahu: " + kidsLeftBehind);
    }

    public static void begin( int adults, int children, BoatGrader b ) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		Hawian.clearSimulation();
		Hawian.setupSimulation(adults, children);
		
		AdultItinerary();
		ChildItinerary();
		
		Hawian.startSimulation();
    }
    
    public static class Hawian {

		static enum Location {
			Oaho, Molokia
		};
		
		static enum Type {
			Adult, Child
		};
		
		static int numPeopleOnOahu = 0;
		static int numAdultsOnOahu = 0;
    		static int numKids = 0;
    		static int numAdults = 0;
    		static Hawian captain = null;
    		static Hawian childCaptain = null;
    		static Hawian passenger = null;
    		static Hawian skipper = null;
    		static Hawian[] kids = null;
    		static Hawian[] adults = null;
    		static Location boatLocation = Location.Oaho;
    		
    		static Lock lock = new Lock();
    		static Condition2 capitanCondition = null;
    		static Condition2 passengerCondition = null;
    		static Condition2 childrenOnOahu = null;
    		static Condition2 passengers = null;
    		static Condition2 simulation = null;
    		static Condition2 hawians = null;
    		static Condition2 adultsOnOahu = null;
    		static Condition2 skipperCondition = null;
    		
    		static KThread simulationThread = null;
    		static boolean setup = false;
    		static boolean childCaptainPicked = false;
    		static boolean debugging = false;
    		
    		String name = "";
    		Location myLocation = Location.Oaho;
    		KThread mythread = null;
    		Type type;
    		boolean started = false;
    	
    		private Hawian() {
    		}
    		
    		/**
    		 * Crate the adults and children. Set class properties.
    		 * @param adults
    		 * @param children
    		 */
    		static void setupSimulation(int adults, int children) {
    			numPeopleOnOahu = adults + children;
    			numAdultsOnOahu = adults;
    			numAdults = adults;
    			numKids = children;
    			Hawian.adults = new Hawian[adults];
    			kids = new Hawian[children];
    			
    			lock = new Lock();
        		capitanCondition = new Condition2(lock);
        		passengerCondition = new Condition2(lock);
        		childrenOnOahu = new Condition2(lock);
        		passengers = new Condition2(lock);
        		simulation = new Condition2(lock);
        		hawians = new Condition2(lock);
        		adultsOnOahu = new Condition2(lock);
        		skipperCondition = new Condition2(lock);
        		
        		createChildren();
        		createAdults();
        		
        		setup = true;
    		}
    		
    		/**
    		 * Clears class properties.
    		 */
    		static void clearSimulation() {
    			numPeopleOnOahu = 0;
    			numAdultsOnOahu = 0;
    	    		numKids = 0;
    	    		numAdults = 0;
    	    		captain = null;
    	    		childCaptain = null;
    	    		passenger = null;
    	    		skipper = null;
    	    		kids = null;
    	    		adults = null;
    	    		boatLocation = Location.Oaho;

    			lock = null;
        		capitanCondition = null;
        		passengerCondition = null;
        		childrenOnOahu = null;
        		passengers = null;
            	simulation = null;
            	hawians = null;
            	adultsOnOahu = null;
            	skipperCondition = null;
            	
            	simulationThread = null;
    	    		setup = false;
    	    		childCaptainPicked = false;
    		}
    		
    		/**
    		 * Creates all the child instances of Hawian for the simulations.
    		 */
    		static private void createChildren() {
    			
    			for(int index = 0; index < numKids; index++)
    				kids[index] = Hawian.createChild("child" + index);
    			
    		}
    		
    		/**
    		 * Creates all the adult instances of Hawian for the simulation.
    		 */
    		static private void createAdults() {

    			for(int index = 0; index < numAdults; index++)
    				adults[index] = Hawian.createAdult("adult" + index);
    			
    		}
    	
    		static Hawian createChild(String name) {
    			log("createChild() start: " + name);
    			
    			final Hawian child = new Hawian();
    			child.name = name;
    			child.type = Type.Child;
    			child.mythread = new KThread(new Runnable() {
				@Override
				public void run() {

					log("starting child thread: " + child.name);
					lockAcquire();
					
					hawians.sleep();

					lockRelease();
					
					child.startChild();
					
					log("ending child thread: " + child.name);
					
				}
				
			}).setName(name);
    			
    			logClassStats();
    			
    			return child;
    		}
    		
    		static Hawian createAdult(String name) {
    			log("createAdult() start: " + name);
    			
    			final Hawian adult = new Hawian();
    			adult.name = name;
    			adult.type = Type.Adult;
    			adult.mythread = new KThread(new Runnable() {
				@Override
				public void run() {
					
					log("starting adult thread: " + adult.name);
					lockAcquire();
					
					hawians.sleep();
					
					lockRelease();
					
					adult.startAdult();
					
					log("ending adult thread: " + adult.name);
					
				}
				
			}).setName(name);
    			
    			logClassStats();
    			
    			return adult;
    		}
    		
    		static void startAdults() {
    			log("startAdults()");

    			for(int index = 0; index < numAdults; index++)
    				adults[index].start();
    			
    		}
    		
    		static void startKids() {
    			log("startKids()");
    			
    			for(int index = 0; index < numKids; index++)
    				kids[index].start();
    			
    		}
    		
    		static void startSimulation() {
    			log("startSimulation()");
    			
    			simulationThread = new KThread(new Runnable() {
					
				@Override
				public void run() {
					log("started simulation thread");
					lockAcquire();
					
					hawians.wakeAll();
					simulation.sleep();
					
					lockRelease();
					log("ended simulation thread");
				}
				
			}).setName("simulationThread");
    			
    			simulationThread.fork();
    			simulationThread.join();
    		}
    		
    		private void start() {
    			if(started == true) return;
    			log("start(), forking thread: " + name);
    			
    			mythread.fork();
    			started = true;
    		}
    		
    		private void startChild() {
    			Lib.assertTrue(this.type == Type.Child, "adult type called startChild()");
    			
    			log("startChild() start: " + name + " location: " + myLocation + " type: " + type);
    			logClassStats();
    			
    			if(childCaptainPicked == false) {
    				
    				while(numPeopleOnOahu != 0) {

            			captainShip();
            			if(numAdultsOnOahu > 0 || skipper != null && skipper.myLocation == Location.Oaho)
            				wakeSkipper();
            			waitForPassenger();
    					sailToMolokai();
    					waitForPassengerToDisembark();
    					if(numPeopleOnOahu > 0) sailBackToOahu();
    					if(numAdultsOnOahu > 0) {
    						
    						giveUpCommand();
    						wakeAdultOnOahu();
    						waitForSkipper();
    						
    					}
    					
    				}
    				
    				finishedSimulation();
    				
    			} else if (skipper == null && numAdultsOnOahu > 0) {
    				
    				skipperShip();
    				while(numAdultsOnOahu > 0 || numPeopleOnOahu > 0 ) {
    					
    					if(myLocation == Location.Oaho) {
    						
	    	    				getOnBoat();
	    	    				waitForCaptain();
	    	    				getOffBoat();
	    	    				wakeCaptain();
	    	    				waitForAdultToSailToOahu();
    						
    					} else {
    						
    						
    						if(numAdultsOnOahu > 0 || myLocation == Location.Molokia) {
    						
    							wakeCaptain();
	    						takeBoat();
	    						sailBackToOahu();
	    						giveUpCommand();
    						
    						} else {
    							
			    				getOnBoat();
    							wakeCaptain();
			    				waitForCaptain();
			    				getOffBoat();
			    				wakeCaptain();
			    				
    						}
    						
    					}
    					
    				}
    				
    				
    			} else {
    				
    				waitForAdultsToLeave();
    				waitInLineForBoat();
    				getOnBoat();
    				waitForCaptain();
    				getOffBoat();
    				wakeCaptain();
    				
    			}

    			logClassStats();
    			log("startChild() end: " + name + " location: " + myLocation + " type: " + type);
    		}
    		
    		private void startAdult() {
    			Lib.assertTrue(this.type == Type.Adult, "child type called startAdult()");
    			
    			log("startChild() start: " + name + " location: " + myLocation + " type: " + type);
    			logClassStats();
    			
    			waitForBoatToReturn();
    			takeBoat();
    			adultSailToMolokia();
    			getOffBoat();
    			
    			if(numAdultsOnOahu == 0) wakeChildrenOnOaho();
    			wakeSkipper();
    			
    			logClassStats();
    			log("startChild() end: " + name + " location: " + myLocation + " type: " + type);
    		}
    		
    		private void captainShip() {
//    			Lib.assertTrue(this.myLocation == boatLocation, "captain should be in the same place as boat");
    			logMe("captainShip()");
    			
    			if(captain == null) captain = this;
    			if(childCaptainPicked == false && this.type == Type.Child) {
    				childCaptainPicked = true;
    				childCaptain = this;
    			}
    			
    		}
    		
    		private void skipperShip() {
    			Lib.assertTrue(captain != this, "captain should not be the skipper");
    			logMe("skipperShip()");
    			
    			if(skipper == null) skipper = this;
    		}
    		
    		private void giveUpCommand() {
    			Lib.assertTrue(captain == this, "only the captain should call this method");
    			logMe("giveUpCommand()");
    			
    			captain = null;
    		}
    		
    		private void wakeAdultOnOahu() {
    			Lib.assertTrue(childCaptain == this || skipper == this, "only the skipper or captain should call this method");
    			logMe("wakeAdultOnOahu()");
    			
    			lockAcquire();

			startKids();
			KThread.currentThread().yield();
			hawians.wakeAll();
    			adultsOnOahu.wake();
    			
    			lockRelease();
    		}
    		
    		private void waitForSkipper() {
    			Lib.assertTrue(this.type == Type.Child, "only a children should call this method");
    			Lib.assertTrue(this.myLocation == Location.Oaho, "only when Oaho should we call this method");
    			Lib.assertTrue(childCaptain == this, "only the captain should call this method");
    			logMe("waitForSkipper()");
    			
    			lockAcquire();
    			
    			while(skipper == null || skipper.myLocation == Location.Molokia)
    				capitanCondition.sleep();
    			
    			lockRelease();
    			
    		}
    		
    		private void waitForAdultToSailToOahu() {
    			Lib.assertTrue(this.type == Type.Child, "only a children should call this method");
    			Lib.assertTrue(skipper == this, "only the skipper should call this method");
    			logMe("waitForAdultToSailToOahu()");
    			
    			lockAcquire();
    			
    			skipperCondition.sleep();
    			
    			lockRelease();
    			
    		}
    		
    		private void takeBoat() {
    			Lib.assertTrue(this.type == Type.Adult || skipper == this, "only an adult or the skipper should call this method");
    			logMe("takeBoat()");
    			
    			captain = this;
    		}
    		
    		private void adultSailToMolokia() {
    			Lib.assertTrue(this.type == Type.Adult, "only an adult should call this method");
    			Lib.assertTrue(this.myLocation == Location.Oaho, "only when on Oaho should we call this method");
    			logMe("adultSailToMolokia()");
    			
    			bg.AdultRowToMolokai();
    			boatLocation = Location.Molokia;
    		}
    		
    		private void waitForBoatToReturn() {
    			Lib.assertTrue(this.type == Type.Adult, "only an adult should call this method");
    			Lib.assertTrue(this.myLocation == Location.Oaho, "only when on Oaho should we call this method");
    			logMe("waitForBoatToReturn()");
    			lockAcquire();
    			
    			while(skipper == null || skipper.myLocation == Location.Oaho)
    				adultsOnOahu.sleep();
    			
    			lockRelease();
    		}
    		
    		private void wakeSkipper() {
    			Lib.assertTrue(this.type == Type.Adult || this == captain, "only an adult or the captain should call this method");
    			logMe("wakeSkipper()");
    			lockAcquire();
    			
    			skipperCondition.wake();
    			
    			lockRelease();
    		}
    		
    		private void waitForPassenger() {
    			Lib.assertTrue(captain == this || childCaptain == this, "only the captain should call this method");
    			logMe("waitForPassenger()");
    			lockAcquire();
    			logClassStats();
    			
    			for(int index = 0; index < numKids; index++)
    				log("KIDS: " + kids[index].myLocation + " " + kids[index].name + " ");
    			
    			while(passenger == null && numPeopleOnOahu > 1) {
    				startKids();
    				KThread.currentThread().yield();
    				hawians.wakeAll();
    				passengers.wake();
    				capitanCondition.sleep();
    			}
    			
    			lockRelease();
    		}
    		
    		private void sailToMolokai() {
    			Lib.assertTrue(captain == this, "only the captain should call this method");
    			logMe("sailToMolokai()");
    			lockAcquire();
    			logClassStats();
    			
    			bg.ChildRowToMolokai();
    			numPeopleOnOahu -= 1;
    			myLocation = Location.Molokia;
    			boatLocation = Location.Molokia;
    			
    			logClassStats();
    			lockRelease();
    		}
    		
    		private void waitForPassengerToDisembark() {
    			Lib.assertTrue(captain == this, "only the captain should call this method");
    			logMe("waitForPassengerToDisembark()");
    			lockAcquire();
    			
    			while(passenger != null) {
	    			passengerCondition.wake();
	    			capitanCondition.sleep();
    			}
    			
    			lockRelease();
    		}
    		
    		private void sailBackToOahu() {
    			Lib.assertTrue(childCaptain == this || skipper == this, "only the child captain or skipper should call this method");
    			logMe("sailBackToOahu()");
    			
    			bg.ChildRowToOahu();
    			numPeopleOnOahu += 1;
    			myLocation = Location.Oaho;
    			boatLocation = Location.Oaho;
    		}
    		
    		private void finishedSimulation() {
    			Lib.assertTrue(captain == this, "only the captain should call this method");
    			logMe("finishedSimulation()");
    			
    			if(numPeopleOnOahu != 0) return;
    			
    			System.out.println("finished simulation");
    			
    			lockAcquire();
    			simulation.wake();
    			lockRelease();
    		}
    		
    		private void waitForAdultsToLeave() {
    			Lib.assertTrue(type == Type.Child, "only children should call this method");
    			logMe("waitForAdultsToLeave()");
    			lockAcquire();
    			
    			while(numAdultsOnOahu > 0)
    				childrenOnOahu.sleep();
    			
    			lockRelease();
    		}
    		
    		private void waitInLineForBoat() {
    			Lib.assertTrue(captain != this, "captain should not call this method");
    			Lib.assertTrue(myLocation == Location.Oaho, "only on people form Oaho should call this method");
    			logMe("waitInLineForBoat()");
    			lockAcquire();
    			
    			while(passenger != null || boatLocation == Location.Molokia)
    				passengers.sleep();
    			
    			lockRelease();
    		}
    		
    		private void getOnBoat() {
    			Lib.assertTrue(captain != this, "captain should not call this method");
    			Lib.assertTrue(myLocation == Location.Oaho, "only on people form Oaho should call this method");
    			
    			passenger = this;
    		}
    		
    		private void waitForCaptain() {
    			Lib.assertTrue(captain != this, "captain should not call this method");
    			Lib.assertTrue(passenger == this, "only the passenger should call this method");
    			Lib.assertTrue(myLocation == Location.Oaho, "only on people form Oaho should call this method");
    			logMe("waitForCaptain()");
    			lockAcquire();
    			
    			while(captain == null || myLocation == captain.myLocation) {
	    			capitanCondition.wake();
	    			passengerCondition.sleep();
    			}
    			
    			lockRelease();
    		}
    		
    		private void getOffBoat() {
    			Lib.assertTrue(childCaptain != this, "captain should not call this method");
    			Lib.assertTrue(myLocation == Location.Oaho, "only on people form Oaho should call this method");
    			logMe("getOffBoat()");

    			logClassStats();
    			
    			if(type == Type.Adult) {
    				
    				captain = null;
    				numAdultsOnOahu -= 1;
    				
    			} else {
    				
    				bg.ChildRideToMolokai();
    				
    			}
    			
    			numPeopleOnOahu -= 1;
    			myLocation = Location.Molokia;
    			passenger = null;
    			

    			logClassStats();
    		}
    		
    		private void wakeChildrenOnOaho() {
    			Lib.assertTrue(type == Type.Adult, "only adults should call this method");
    			Lib.assertTrue(numAdultsOnOahu == 0, "number of adults on Oahu must be zero");
    			logMe("wakeChildrenOnOaho()");
    			lockAcquire();
    			
    			childrenOnOahu.wakeAll();
    			
    			lockRelease();
    		}
    		
    		private void wakeCaptain() {
    			Lib.assertTrue(captain != this, "captain should not call this method");
    			Lib.assertTrue(myLocation == Location.Molokia || this == skipper, "only on people form Molokia or the shipper should call this method");
    			logMe("wakeCaptain()");
    			lockAcquire();
    			
    			capitanCondition.wake();
    			
    			lockRelease();
    		}
    		
    		private static void lockAcquire() {
    			lock.acquire();
    		}
    		
    		private static void lockRelease() {
    			lock.release();
    		}
    		
    	    
    	    /**
    	     * Log messages to terminal if debugging is set to true or 'nachos' command is executed with the d flag.
    	     * @param msg String to print out
    	     */
    	    private static void log(String msg) {
    	    		Lib.debug('d', msg);
    	    		
    	    		if(debugging == false) return;
    	    		System.out.println(msg);
    	    }
    	    
    	    private static void logClassStats() {
    	    		if(captain != null) log("captain: " + captain.name);
    	    		if(passenger != null) log("passenger: " + passenger.name);
    	    		if(skipper != null) log("skipper: " + skipper.name);
    	    		log("poeple on Oahu: " + numPeopleOnOahu);
    	    		log("adults on Oahu: " + numAdultsOnOahu);
    	    		log("number of kids: " + numKids);
    	    		log("number of adults: " + numAdults);
    	    		log("boat location: " + boatLocation);
    	    }
    	    
    	    private void logMe(String msg) {
    	    		log(msg + " " + name);
    	    }
    	
    } 

    static void AdultItinerary() {
		// bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE. 
    		
		/* This is where you should put your solutions. Make calls
		   to the BoatGrader to show that it is synchronized. For
		   example:
		       bg.AdultRowToMolokai();
		   indicates that an adult has rowed the boat across to Molokai
		*/
    		Hawian.startAdults();
    }

    static void ChildItinerary() {
		// bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE. 
    		Hawian.startKids();
    }

    static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
    }
    
}