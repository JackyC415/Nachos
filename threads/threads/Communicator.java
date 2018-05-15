package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
		log("speak() called, thread: " + KThread.currentThread().getName() + " word: " + word);

        Machine.interrupt().disable();
        comLock.acquire();
        num_speakers += 1;
		
        while(num_listeners == 0 || my_word != null) speakers.sleep();
        
        my_word = word;
        num_speakers -= 1;
        listeners.wake();
        comLock.release();

        // wait for listen to return
		KThread.currentThread().yield();

        Machine.interrupt().enable();
        
		log("speak() ended, thread: " + KThread.currentThread().getName() + " word: " + word);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    		log("listen() called, thread : " + KThread.currentThread().getName());

        Machine.interrupt().disable();
        comLock.acquire();
        num_listeners += 1;
        
        while(my_word == null) {
        	
            if(num_speakers > 0) speakers.wake();
        		listeners.sleep();
        		
        }
        
        num_listeners -= 1;
        int temp_word = my_word;
        my_word = null;
		
        comLock.release();
        Machine.interrupt().enable();
        
		log("listen() ended, thread :" + KThread.currentThread().getName());
        return temp_word;
    }
    
    /**
     * Log messages to terminal if debugging is set to true or 'nachos' command is executed with the d flag.
     * @param msg String to print out
     */
    private void log(String msg) {
    		Lib.debug('d', msg);
    		
    		if(DEBUGGING == false) return;
    		System.out.println(msg);
    }

    static private boolean DEBUGGING = false;
    private int num_speakers = 0;
    private int num_listeners = 0;
    private Integer my_word = null;
    Lock comLock = new Lock();
    Condition2 listeners = new Condition2(comLock);
    Condition2 speakers = new Condition2(comLock);
    
    static public class Tester {
    		static private Tester tester;
    		
    		private Tester() {
    		}
    		
    		static public Tester CreateTester() {
    			if(tester == null) tester = new Tester();
    			return tester;
    		}
    		
    	    public static void selfTest() {
        		System.out.println("Communicator selfTest()");
        		
        		DEBUGGING = false;
        		
        		testMessageSent();
        		testListenerWaitingForSpeaker();
        		testSendingMultipleMessages();
        		testWithOnlySpeakers();
        		testWithOnlyListeners();
        		testWithSpeakerAndMultipleListeners();
        		testWithSpeakersAndListener();
        		testListenReturnsOnceIfSpokenOnce();
        }
    	    
    	    private static KThread createSpeaker(final String name, final Communicator comm, final int word) {
	    		Runnable run = new Runnable() {
				
				@Override
				public void run() {
					
					comm.speak(word);
					
				}
				
			};
			return createThread(name, run);
    	    }
    	    
    	    private static KThread createListener(final String name, final Communicator comm, final int expectedWord) {
    	    		Runnable run = new Runnable() {
				
				@Override
				public void run() {
					
					int actualWord = comm.listen();
					checkWord(expectedWord, actualWord);
					
				}
				
			};
			return createThread(name, run);
    	    }
    	    
    	    private static KThread createThread(String threadName, Runnable run) {
	    		return new KThread(run).setName(threadName);
    	    }
    	    
    	    private static void checkWord(int expectedWord, int actualWord) {
				Lib.assertTrue(expectedWord == actualWord, "expected word " + expectedWord + " was different than output of listen() " + actualWord + " - TEST FAILED");
    	    }
        
    	    public static void testListenReturnsOnceIfSpokenOnce() {
    	    		String testName = "Communicator: test listen() returns once per speaker";
	    		System.out.println(testName);
	    		final int expectedWord = 324;
    			final Communicator comm = new Communicator();
	    	
    			Runnable runSpeaker = new Runnable() {
				@Override
				public void run() {
					
					System.out.println("running speaker");
					comm.speak(expectedWord);
					System.out.println("ending speaker");
					
				}
			};
	    	
			Runnable runListener = new Runnable() {
				@Override
				public void run() {
					
					System.out.println("running listener");
					
					int word = comm.listen();
					System.out.println("listener returned once " + word);
					
				}
			};
	    	
			Runnable runListener2 = new Runnable() {
				@Override
				public void run() {
					
					System.out.println("running listener2");
					
					int word = comm.listen();
					throw new AssertionError("listener2, listen() returned twice " + word); 
					
				}
			};
    			
	    		KThread speaker = createThread("speaker", runSpeaker);
	    		speaker.fork();

	    		KThread listener = createThread("listener", runListener);
	    		listener.fork();
	    		
	    		KThread listener2 = createThread("listener2", runListener2);
	    		listener2.fork();
	    		
	    		KThread.currentThread().yield();
	    		KThread.currentThread().yield();
	    		KThread.currentThread().yield();
	    		
	    		System.out.println(testName + " - TEST PASSED");
    	    }
    	    
        public static void testMessageSent() {
	    		System.out.println("Communicator: test that message is sent");
	    		final int expectedWord = 100;
    			final Communicator comm = new Communicator();
	    	
	    		KThread speaker = createSpeaker("speaker", comm, expectedWord);
	    		speaker.fork();
	    		KThread.currentThread().yield();

    			Lib.assertTrue(comm.num_speakers == 1, "Communictor's number of speakers was not updated to one after speak() called.");
    			
	    		KThread listener = createListener("listener", comm, expectedWord);
	    		listener.fork();
	    		KThread.currentThread().yield();
	    		
	    		// yield for speaker
	    		KThread.currentThread().yield();
	    		
	    		// yield for listener
	    		KThread.currentThread().yield();
	    		
    			Lib.assertTrue(comm.num_listeners == 0, "Communictor's number of listeners was not reset after listen() called.");
	    		
	    		System.out.println("Communicator: test that message is sent - TEST PASSED");
        }
        
        public static void testListenerWaitingForSpeaker() {
	    		System.out.println("Communicator: test listener waiting for speaker");
	    		final int expectedWord = 100;
    			final Communicator comm = new Communicator();
    			
	    		KThread listener = createListener("listener", comm, expectedWord);
	    		listener.fork();
	    		
	    		// wait for listener thread to call listen()
	    		KThread.currentThread().yield();

    			Lib.assertTrue(comm.num_listeners == 1, "Communictor's number of speakers was not incremented after listen() was called - TEST FAILED");
	    	
	    		KThread speaker = createSpeaker("speaker", comm, expectedWord);
	    		speaker.fork();
	    		
	    		// wait for speaker thread to call speak()
	    		KThread.currentThread().yield();
	    		
	    		// wait for the listen thread to send
	    		KThread.currentThread().yield();
	    		
    			Lib.assertTrue(comm.num_listeners == 0, "Communictor's number of speakers was not derecemented after speak() was called - TEST FAILED");
	    		
	    		
	    		System.out.println("Communicator: test listener waiting for speaker - TEST PASSED");
        }
        
        public static void testSendingMultipleMessages() {
	    		System.out.println("Communicator: test that multiple messages can be sent");
	    		final int expectedWord = 1818, expectedWord1 = 1868, expectedWord2 = 134;
    			final Communicator comm = new Communicator();
    			
    			Runnable tewodros2 = new Runnable() {
				
				@Override
				public void run() {
					
					comm.speak(expectedWord);
					comm.speak(expectedWord1);
					
					// switch roles
					int actualWord = comm.listen();
					checkWord(expectedWord2, actualWord);
		    			System.out.println("Communicator: test that multiple messages can be sent, message 3 - TEST PASSED");
					
				}
				
			};
			
			Runnable victoria = new Runnable() {
			
				@Override
				public void run() {
					
					// test 1
					int actualWord = comm.listen();
					checkWord(expectedWord, actualWord);
		    			System.out.println("Communicator: test that multiple messages can be sent, message 1 - TEST PASSED");
					// wait for tewodros2 to call speak()
		    			KThread.currentThread().yield();
		    			
					// test 2
					actualWord = comm.listen();
					checkWord(expectedWord1, actualWord);
		    			System.out.println("Communicator: test that multiple messages can be sent, message 2 - TEST PASSED");
					
		    			// wait for tewodros2 to call listen()
		    			KThread.currentThread().yield();
		    			
					// switch roles
					comm.speak(expectedWord2);
					
				}
				
			};
			
	    		KThread speaker = createThread("speaker", tewodros2);
	    		speaker.fork();
	    		KThread.currentThread().yield();
	    		
	    		KThread listener = createThread("listener", victoria);
	    		listener.fork();
	    		KThread.currentThread().yield();
        }
        
        public static void testWithOnlySpeakers() {
    			System.out.println("Communicator: test with only speakers");
    			final Communicator comm = new Communicator();
    			final int word1 = 123, word2 = 321;

			Runnable run1 = new Runnable() {
			
				@Override
				public void run() {
					
					comm.speak(word1);
    					System.out.println("Communicator: test with only speakers, speaker got control back with no listeners - TEST FAILED");
					
				}
			};

			Runnable run2 = new Runnable() {
			
				@Override
				public void run() {
					
					comm.speak(word2);
    					System.out.println("Communicator: test with only speakers, speaker got control back with no listeners - TEST FAILED");
					
				}
			};
			
			KThread speaker1 = createThread("speaker1", run1),
					speaker2 = createThread("speaker2", run2);
			
    			try {
    				
					speaker1.fork();
					speaker2.fork();
					
					// let speaker1 call speak
					KThread.currentThread().yield();

					// let speaker2 call speak
//					KThread.currentThread().yield();

					Lib.assertTrue(comm.num_speakers == 2, "Communicator: test with only speakers, wrong number of speakers " + comm.num_speakers + " - TEST FAILED");
					
					Lib.assertTrue(comm.num_listeners == 0, "Communicator: test with only speakers, wrong number of listeners " + comm.num_listeners + " - TEST FAILED");
					
					Lib.assertTrue(comm.my_word == null, "Communicator: test with only speakers, word should be null until listen() is called - TEST FAILED");
    				
    				System.out.println("Communicator: test with only speakers - TEST PASSED");
    			
    			} catch(Exception e) {
    				
    				System.out.println("Communicator: test with only speakers, exception thrown" + e.getLocalizedMessage() + " - TEST FAILED");
    				e.printStackTrace();
    				
    			}
    			
        }
        
        public static void testWithOnlyListeners() {
    			System.out.println("Communicator: test with only listeners");
			final Communicator comm = new Communicator();

			Runnable run = new Runnable() {
			
				@Override
				public void run() {
					
					comm.listen();
					System.out.println("Communicator: test with only listeners, listener got control back with no speakers - TEST FAILED");
					
				}
			};
			
			KThread listener1 = createThread("listener1", run),
					listener2 = createThread("listener2", run);
			
				try {
					
					listener1.fork();
					listener2.fork();
					
					// let listener1 call speak
					KThread.currentThread().yield();

					// let listener2 call speak
					KThread.currentThread().yield();

					Lib.assertTrue(comm.num_speakers == 0, "Communicator: test with only listeners, wrong number of speakers " + comm.num_speakers + " - TEST FAILED");
					
					Lib.assertTrue(comm.num_listeners == 2, "Communicator: test with only listeners, wrong number of listeners " + comm.num_listeners + " - TEST FAILED");
					
					Lib.assertTrue(comm.my_word == null, "Communicator: test with only listeners, wrong word " + comm.my_word + " - TEST FAILED");
					
					System.out.println("Communicator: test with only listeners - TEST PASSED");
				
				} catch(Exception e) {
					
					System.out.println("Communicator: test with only listeners, exception thrown" + e.getLocalizedMessage() + " - TEST FAILED");
					e.printStackTrace();
					
			}
        }
        
        public static void testWithSpeakerAndMultipleListeners() {
    			System.out.println("Communicator: test with one speaker and multiple listeners");
    			final int expectedWord = 100;
			final Communicator comm = new Communicator();
    		
	    		KThread listener1 = createListener("listener1", comm, expectedWord);
	    		listener1.fork();
	    		KThread.currentThread().yield();
	
	    		KThread listener2 = createListener("listener2", comm, expectedWord);
	    		listener2.fork();
	    		KThread.currentThread().yield();
	    	
	    		KThread speaker = createSpeaker("speaker", comm, expectedWord);
	    		speaker.fork();
	    		KThread.currentThread().yield();
	    		
    			System.out.println("Communicator: test with one speaker and multiple listeners - TEST PASSED");
        }
        
        public static void testWithSpeakersAndListener() {
	    		System.out.println("Communicator: test with multiple speakers and one listener");
	    		final Communicator comm = new Communicator();
	    		final int wrongWord1 = 2143, wrongWord2 = 234, expectedWord = 103;
	    		KThread speaker1 = createSpeaker("speaker1", comm, expectedWord),
	    				speaker2 = createSpeaker("speaker2", comm, wrongWord1),
					speaker3 = createSpeaker("speaker3", comm, wrongWord2),
					listener = createListener("listener1", comm, expectedWord);
	    		
	    		speaker1.fork();
	    		speaker2.fork();
	    		speaker3.fork();
	    		
	    		KThread tester = KThread.currentThread();
	    		tester.yield();
	    		
	    		listener.fork();
	    		
	    		tester.yield();
	    		
	    		System.out.println("Communicator: test with multiple speakers and one listener - TEST PASSED");
        }
    	
    }
}