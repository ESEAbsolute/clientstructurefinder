package icu.eseabs0.clientstructurefinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import icu.eseabs0.clientstructurefinder.command.FindStructureCommand;

public class ClientStructureFinderClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(FindStructureCommand::register);
    }
}
