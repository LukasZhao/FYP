package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;
import java.util.Random;

public class RandomPlayer extends AbstractPlayer {

    /**
     * Random generator for this agent.
     */
    private final Random rnd;

    public RandomPlayer(Random rnd)
    {
        this.rnd = rnd;
    }

    public RandomPlayer()
    {
        this(new Random());
    }

    @Override
    public AbstractAction getAction(AbstractGameState observation) {
        int randomAction = rnd.nextInt(observation.getActions().size());
        List<AbstractAction> actions = observation.getActions();
        return actions.get(randomAction);
    }

    @Override
    public String toString() {
        return "Random";
    }
}