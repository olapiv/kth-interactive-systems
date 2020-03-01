# Game of Tag for Mobile Agents

This is lab TAG, the game of Tag for mobile agents. It consists of:

* bailiff - A Jini-aware execution service for mobile Java code.
* dexter - A small agent that jumps randomly between Bailiffs

## Install

1. Install ant (http://ant.apache.org/)
2. Compile sources and compose the jar-files with the develop/build.xml file using ant: ```ant```.
3. Install jar-files in test branch: ```ant install```

Run ```ant clean``` to delete generated files.

On a Windows system you can also compile and install by using the BAT-files in the develop/bat directory.

## Run

### Unix

1. Run ```test/bin/unix/r1_httpd.sh```
2. Run ```test/bin/unix/r2_rmid.sh```
3. Run ```test/bin/unix/r3_reggie.sh```
4. Run three times with different ids: ```lab2v13/test/bin/unix/bailiff.sh -id b1```
5. Run ```lab2v13/test/bin/unix/dexter.sh -id d1 -debug```

### Windows

1. Run ```test/bin/pc/r1_httpd.bat```
2. Run ```test/bin/pc/r2_rmid.bat```
3. Run ```test/bin/pc/r3_reggie.bat```
4. Run three times with different ids: ```lab2v13/test/bin/unix/bailiff.bat -id b1```
5. Run ```lab2v13/test/bin/unix/dexter.bat -id d1 -debug```

## The TAG assignment

The challenge is to create the Game of Tag for mobile software agents.

The game of tag is a child's game, played minimally by two players. It is very simple. One player is choosen to be 'it'. That player's task is to run up to one of the other players and touch him or her, saying "You're it!", whereupon the 'it' property and the associated task is transferred to the other player. The players who are currently not 'it', try to evade being tagged, usually by running and hiding.

In this assignment, the agents ("Dexters") can be hosted by the execution servers ("Bailiffs"). The agent that "is it" can only tag other agents that are being hosted on the same Bailiff.

### Requirements of Task

1. Implement the game of tag for >3 mobile agents and >3 Bailiffs (execution servers)
2. Tagging can only be done between players in the same Bailiff
3. The tag (the 'it' property) must be passed reliably from one player to another. It must not be lost or duplicated during the transaction.

The Jini middleware can help each player to dynamically discover and communicate with Bailiffs.

When a mobile object (e.g. a player) 'moves' from one Bailiff to another, it does not really move at all. It sends a copy of itself to the other Bailiff, and if the copy was successful, the original object terminates its thread of execution and goes to garbage collection. Thus, if a player is tagged at the wrong moment, the copy of the player that moved away stays untagged, while the original object instance is tagged instead. When the original instance dies, it takes the 'it' property with it into oblivion, and the tag is lost from the game.

### Let the Bailiff mediate player-to-player communication

It is strongly advised that communication between players use the Bailiff as a proxy. The central message between players in the game of Tag is of course the tagging action itself. It could look something like this:

```Java
  // In this code example, AgentID shold be replaced with whatever is
  // chosen as the identifier datatype. The class java.util.UUID is a
  // good choice.


  // In the BailiffInterface, which is what a player sees:
  public boolean tag(AgentID aid) throws java.rmi.RemoteException;


  // Which the Bailiff implements thus:
  public boolean tag(AgentID aid) throws java.rmi.RemoteException
  {
    Agent a = lookup(aid);
    return a.tag();
  }

  // In the player when being tagged:
  public boolean tag()
  {
    if (/* tag succeeded */)
      // update my state to be 'it'
      return true;
    else
      return false;
  }

  // In the player when trying to tag another player:
  ...
  BailiffInterface myBailiff // Bailiff I am in
  AgentID victim             // ID of other player to tag
  ...
  try {
    if (myBailiff.tag(victim))
      // update my state to be not it
  }
  catch (RemoteException rex) {}
```

In the above example, the Bailiff uses the provided AgentID to locate the requested player, and then perform the local method call. Then it is the player being tagged that decides if it was tagged or not, and returns true or false accordingly. If the requested agent is no longer available in the Bailiff, the lookup will return null, and the calling player receives a NullPointerException wrapped inside a RemoteException.

It should also be realised, that when the Bailiff is calling the player's tag() method, the player's own thread of execution is also active in the player object. So if the player is using, for example, a boolean variable to keep track of its 'it' status, the code that checks that variable for what to do and how to behave, should expect that status to go from 'not it' to 'it' at any time.

Using the Bailiff as a message proxy is good, but it is not the only way. Other messaging schemes could be devised. However, there is a particularly dangerous bug-fest luring in another modification of the BailiffInterface. In that design, the Bailiff provides a reference to the requested player, and the tagging player calls the victim directly (except it does not):

```Java
  /* *** DO NOT USE *** */

  // BailiffInterface
  public Agent getAgent(AgentID aid) throws java.rmi.RemoteException;

  // Bailiff
  public Agent getAgent(AgentID aid) throws java.rmi.RemoteException
  {
    return lookup(aid);
  }

  // Player being it
  ...
  Agent a = myBailiff.getAgent(victimID); // get the victim
  if (a != null && a.tag())               // tag it
    isIt = false;                         // I am no longer 'it'
  ...
```

The programmer thinks: "Well, I know the it player and the victim are on the same Bailiff, so I receive a reference to the victim, so I can call its tag method to tag it." But here is the trap: the BailiffInterface is a remote interface. It cannot return a local reference. It will return a reference to a copy of the victim, and it is the copy that gets tagged. The original player object is unmodified, and so the tag is lost.

One important consequence of having the Bailiff act as mediator, is that it must recognise the player class. Otherwise it cannot call a Player.tag() method. This is another necessary modification of the Bailiff, and it can be implemented in at least two ways:

The first solution is to simply use the player class in the Bailiff code, and cast to it where required. A more elegant approach, however, is to have the player implement an interface which the Bailiff knows about. That way one can have players from different classes participate in the game together.

Finally, the Bailiff will need a data structure that maps from player identifiers (AgentID) to the local objects that are the players themselves. The player should be added to the map when the Agitator starts, and removed when it stops, also when the player stops because of an exception. The data structure must be protected against concurrent modification, because at any time a present player may die or a remote player migrate to the Bailiff. A suitable instance of interface java.util.Map is java.util.HashMap.

Another bug from past years is if the Bailiff fails to remove done players from the map it maintains. Not only will the map continue to grow and consume memory, but it will also accumulate a mausoleum of dead player instances, which can be tagged but will never tag back. Once a dead player has been tagged, the tag is lost.

It must also be possible to create a list of the names of the active players currently in the Bailiff. For the namelist, start looking at method java.util.Map.keySet(). Do not send the whole map instance as the response, extract the names into something nice, like an array of String, or something which implements interface java.util.List. If you decide to annotate the list elements with information like, 'this is you' and 'this is the it player', you probably want to create an element class. Remember to protect against concurrent modification while extracting the names.

### Player behaviours

The player has two hard states, being 'it' or 'not it'. In the 'it' state, it hunts down victims and tries to tag them. A successful predator would probably aim for the Bailiff with the most players in it, as this is a simple heuristic to implement.

An untagged player, on the other hand, has more freedom in how to behave. Obviously it needs to watch out for the player being 'it', and attempt to escape if it arrives in the current Bailiff. But if not immediately threatened, it has a choice between staying put where it is, or hop over to some other safe Bailiff. If it decides to hop on, it has further choices over a random Bailiff, or one with few players, or one with many players.

The behaviour or strategy exhibited by players, will dramatically affect the overall appearence of the game. Players that seek out company, will huddle together in some corner of the playfield, leaving most of the Bailiffs empty. Players that seek loneliness will spread out, automatically balancing the load on the available Bailiffs.

There is no requirement to implement alternative non-tagged behaviours, but you are very welcome to experiment with them and observe the emergent system-wide effects.

### Player delays

In order to be able to follow the progress of a game at human speeds delays are required. It is difficult to give good recommendations for these, but a general move delay of 3-5 seconds may be a good starting point. You may also want to give each player a bit of random variation on the order of 50-250 ms so that they slide out of lock-step behaviour. In particular, a player being 'it' could be awarded a slightly shorter delay to give it some edge and make successful tagging more likely.

### Examination

For lab TAG you must do the following:

* Implement the game of Tag using the provided material.
* Write, print and submit a short report on your design choices, and problems encountered and solved. Remember to put the following on the title sheet:
  * ID2010/LAB TAG
  * Your names
* Give an oral presentation and show a running implementation. The result must execute and demonstrate (textually or graphically) that the game is being played by three players on a playfield of three Bailiffs. [The group and the examiner sits down together at the computers.  The group runs their system and explains briefly what they have done. The examiner asks questions as necessary.]
