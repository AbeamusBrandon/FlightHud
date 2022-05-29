package net.torocraft.flighthud.sounds;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.torocraft.flighthud.FlightHud;

public class ModSounds {
    public static SoundEvent ALTITUDE_WARNING = registerSoundEvent("altitude_warning");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier identifier = new Identifier(FlightHud.MODID, name);
        return Registry.register(Registry.SOUND_EVENT, identifier, new SoundEvent(identifier));
    }

    public static void registerSounds() {
        System.out.println("registering sounds for " + FlightHud.MODID);
    }
}
