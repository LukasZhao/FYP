package games.battlelore;

import core.AbstractGameData;
import core.components.*;
import games.battlelore.components.Unit;
import games.battlelore.gui.BattleloreGUI;
import games.pandemic.PandemicData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BattleloreData extends AbstractGameData
{
    private GridBoard board;
    private List<Deck<Card>> decks;
    private List<Unit> units;

    @Override
    public void load(String dataPath)
    {
        //units = Unit.parseComponent();
        units = Unit.loadUnits(dataPath + "\\units.json");

    }

    public List<Unit> getUnits()
    {
        return units;
    }

    public BattleloreData copy()
    {
        BattleloreData data = new BattleloreData();
        data.units = new ArrayList<>();
        for(Unit i : units) data.units.add((Unit)i.copy());

        return data;
    }

}
