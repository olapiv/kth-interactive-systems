// Dexter.java
// 2018-08-15/fki Refactored from v11

package tag.dexter;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.ServiceDiscoveryManager;
import tag.bailiff.BailiffInterface;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Dexter jumps around randomly among the Bailiffs. Dexter can be used
 * to test that the system is operating, and as a template for more
 * evolved agents. Since objects of class Dexter move between JVMs, it
 * must be implement the Serializable marker interface.
 */
public class Dexter implements Serializable {
    /**
     * Identification string used in debug messages.
     */
    private String id = UUID.randomUUID().toString();

    /**
     * Default sleep time so that we have time to track what it does.
     */
    private long restraintSleepMs = 5000;

    /**
     * The jump count variable is incremented each time method topLevel
     * is entered. Its value is printed by the debugMsg routine.
     */
    private int jumpCount = 0;

    /**
     * The default sleep time between subsequent queries of a Jini
     * lookup server.
     */
    private long retrySleep = 20 * 1000; // 20 seconds

    /**
     * The maximum number of results we are interested when asking the
     * Jini lookup server for present bailiffs.
     */
    private int maxResults = 8;

    /**
     * The debug flag controls the amount of diagnostic info we put out.
     */
    protected boolean debug = false;

    /**
     * The string name of the Bailiff service interface, used when
     * querying the Jini lookup server.
     */
    protected static final String bfiName = "tag.bailiff.BailiffInterface";

    /**
     * Dexter uses a Jini ServiceDiscoveryManager to find Bailiffs. The
     * SDM is not serializable so it must recreated each time a Dexter
     * moves to a different Bailiff. By marking the reference variable
     * as transient, we indicate to the compiler that we are aware of
     * that whatever the variable refers to, it will not be serialized.
     */
    protected transient ServiceDiscoveryManager SDM;

    /**
     * This Jini service template is created in Dexter's constructor and
     * used in the topLevel method to find Bailiffs. The service
     * template IS serializable so Dexter only needs to instantiate it
     * once.
     */
    protected ServiceTemplate bailiffTemplate;

    private boolean isIt = false;

    private BailiffInterface currentBailiff;

    /**
     * Sets the id string of this Dexter.
     *
     * @param id The id string. A null argument is replaced with the
     *           empty string.
     */
    public void setId(String id) {
        this.id = (id != null) ? id : "";
    }

    public String getId() {
        return id;
    }

    public boolean isIt() {
        return isIt;
    }

    public void getTagged() {
        debugMsg("I am IT now");
        this.isIt = true;
    }

    public void setCurrentBailiff(BailiffInterface currentBailiff) {
        this.currentBailiff = currentBailiff;
    }

    /**
     * Sets the restraint sleep duration.
     *
     * @param ms The number of milliseconds in restraint sleep.
     */
    public void setRestraintSleep(long ms) {
        restraintSleepMs = Math.max(0, ms);
    }

    /**
     * Sets the query retry sleep duration.
     *
     * @param ms The number of milliseconds between each query.
     */
    public void setRetrySleep(long ms) {
        retrySleep = Math.max(0, ms);
    }

    /**
     * Sets the maximum number of results accepted from the Jini lookup
     * server.
     *
     * @param n The maximum number of results.
     */
    public void setMaxResults(int n) {
        maxResults = Math.max(0, n);
    }

    /**
     * Sets or clears the global debug flag. When enabled, trace and
     * diagnostic messages are printed on stdout.
     */
    public void setDebug(boolean isDebugged) {
        debug = isDebugged;
    }

    /**
     * Outputs a diagnostic message on standard output. This will be on
     * the host of the launching JVM before Dexter moves. Once he has migrated
     * to another Bailiff, the text will appear on the console of that Bailiff.
     *
     * @param msg The message to print.
     */
    protected void debugMsg(String msg) {
        if (debug)
            System.out.printf("%s(%d):%s%n", id, jumpCount, msg);
    }

    /**
     * Creates a new Dexter. All the constructor needs to do is to
     * instantiate the service template.
     *
     * @throws ClassNotFoundException Thrown if the class for the Bailiff
     *                                service interface could not be found.
     */
    public Dexter() throws java.lang.ClassNotFoundException {

        // The Jini service template bailiffTemplate is used to query the
        // Jini lookup server for services which implement the
        // BailiffInterface. The string name of that interface is passed
        // in the bfi argument. At this point we only create and configure
        // the service template, no query has yet been issued.

        bailiffTemplate = new ServiceTemplate(null, new Class[]{java.lang.Class.forName(bfiName)}, null);
    }

    /**
     * Sleep for the given number of milliseconds.
     *
     * @param ms The number of milliseconds to sleep.
     */
    protected void snooze(long ms) {
        try {
            Thread.currentThread().sleep(ms);
        } catch (java.lang.InterruptedException e) {
        }
    }

    /**
     * This is Dexter's main program once he is on his way. In short, he
     * gets himself a service discovery manager and asks it about Bailiffs.
     * If the list is long enough, he then selects one randomly and pings it.
     * If the ping returned without a remote exception, Dexter then tries
     * to migrate to that Bailiff. If the ping or the migration fails, Dexter
     * gives up on that Bailiff and tries another.
     */
    public void topLevel() throws java.io.IOException {
        SDM = new ServiceDiscoveryManager(null, null);
        debugMsg("Sleeping...");
        snooze(isIt ? restraintSleepMs : (long) (restraintSleepMs * 0.8));
        Random rnd = new Random();

        if (isIt && currentBailiff != null) {
            debugMsg("Trying to tag a player...");
            for (String playerId : currentBailiff.getPlayerIds()) {
                if (!playerId.equals(id) && currentBailiff.tag(playerId)) {
                    isIt = false;
                    debugMsg("Player tagged");
                    break;
                }
            }
        }

        while(true) {
            BailiffInterface[] activeBailiffs = findActiveBailiffs();

            if (!isIt) {
                outer:
                while (true) {
                    BailiffInterface nextBailiff = activeBailiffs[rnd.nextInt(activeBailiffs.length)];
                    for (String player : nextBailiff.getPlayerIds()) {
                        if (nextBailiff.isIt(player)) {
                            continue outer;
                        }
                    }
                    jump(nextBailiff);
                    return;
                }
            }

            int maxAgentsInBailiff = -1;
            BailiffInterface fullestBailiff = null;
            for (BailiffInterface bailiff: activeBailiffs) {
                int numOfPlayersInBailiff = bailiff.getPlayerIds().length;
                if (currentBailiff != null && bailiff.getProperty("id").equals(currentBailiff.getProperty("id"))) {
                    numOfPlayersInBailiff--;
                }
                if (numOfPlayersInBailiff > maxAgentsInBailiff) {
                    fullestBailiff = bailiff;
                    maxAgentsInBailiff = numOfPlayersInBailiff;
                }
            }
            if (fullestBailiff != null) {
                jump(fullestBailiff);
                return;
            }
        }
    }   // topLevel

    private void jump(BailiffInterface bfi) {
        try {
            debugMsg("Jumping to: " + bfi.getProperty("id"));
            jumpCount++;
            bfi.migrate(this, "topLevel", new Object[]{});
            SDM.terminate();
        } catch (RemoteException | NoSuchMethodException rex) {
            rex.printStackTrace();
        }
    }

    private BailiffInterface[] findActiveBailiffs() {
        ServiceItem[] svcItems = new ServiceItem[0];
        while (svcItems.length == 0) {
            svcItems = SDM.lookup(bailiffTemplate, maxResults, null);
            if (svcItems.length == 0) {
                debugMsg("No connected bailiffs found. Sleeping...");
                snooze(retrySleep);
            }
        }
        //debugMsg("Found " + svcItems.length + " bailiffs. Pinging all of them...");
        List<BailiffInterface> activeBailiffs = new ArrayList<>();
        for (ServiceItem svcItem : svcItems) {
            if (svcItem.service instanceof BailiffInterface) {
                BailiffInterface bi = (BailiffInterface) svcItem.service;
                try {
                    bi.ping();
                    activeBailiffs.add(bi);
                } catch (java.rmi.RemoteException rex) {
                }
            }
        }
        //debugMsg("There are " + activeBailiffs.size() + " active bailiffs");
        return activeBailiffs.toArray(new BailiffInterface[0]);
    }

    private static void showUsage() {
        String[] msg = {
                "Usage: {?,-h,-help}|[-debug][-id string][-rs ms][-qs ms][-mr n]",
                "? -h -help   Show this text",
                "-debug       Enable trace and diagnostic messages",
                "-id  string  Set the id string printed by debug messages",
                "-rs  ms      Set the restraint sleep in milliseconds",
                "-qs  ms      Set the Jini lookup query retry delay",
                "-mr  n       Set the Jini lookup query max results limit",
                "-it          Mark Dexter as it"
        };
        for (String s : msg)
            System.out.println(s);
    }

    // The main method is only used by the initial launch. After the
    // first jump, Dexter always restarts in method topLevel.

    public static void main(String[] argv)
            throws
            java.io.IOException, java.lang.ClassNotFoundException {

        // Make a new Dexter and configure it from commandline arguments.

        Dexter dx = new Dexter();

        // Parse and act on the commandline arguments.

        int state = 0;

        for (String av : argv) {

            switch (state) {

                case 0:
                    if (av.equals("?") || av.equals("-h") || av.equals("-help")) {
                        showUsage();
                        return;
                    } else if (av.equals("-debug"))
                        dx.setDebug(true);
                    else if (av.equals("-id"))
                        state = 1;
                    else if (av.equals("-rs"))
                        state = 2;
                    else if (av.equals("-qs"))
                        state = 3;
                    else if (av.equals("-mr"))
                        state = 4;
                    else if (av.equals("-it"))
                        dx.isIt = true;
                    else {
                        System.err.println("Unknown commandline argument: " + av);
                        return;
                    }
                    break;

                case 1:
                    dx.setId(av);
                    state = 0;
                    break;

                case 2:
                    dx.setRestraintSleep(Long.parseLong(av));
                    state = 0;
                    break;

                case 3:
                    dx.setRetrySleep(Long.parseLong(av));
                    state = 0;
                    break;

                case 4:
                    dx.setMaxResults(Integer.parseInt(av));
                    state = 0;
                    break;
            }    // switch
        }    // for all commandline arguments

        dx.topLevel();        // Start the Dexter

    } // main
}
