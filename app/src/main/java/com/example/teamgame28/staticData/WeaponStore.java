package com.example.teamgame28.staticData;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Weapon;

import java.util.ArrayList;
import java.util.List;

public class WeaponStore {

    public static List<Weapon> getWeapons() {
        List<Weapon> weapons = new ArrayList<>();

        Weapon sword = new Weapon("sword", "Sword +5% PP", 300,
                0.05, 0.0,R.drawable.swords);
        weapons.add(sword);

        Weapon bow = new Weapon("bow", "Bow +5% Coin Gain", 300,
                0.0, 0.05,R.drawable.bow);

        weapons.add(bow);

        return weapons;
    }
}
