package io.github.satxm.mcwifipnp;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;
import org.quiltmc.qsl.screen.api.client.ScreenEvents;

import io.github.satxm.mcwifipnp.mixin.PlayerListAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;

public class MCWiFiPnP implements ModInitializer {
	public static final String MODID = "mcwifipnp";

	@Override
	public void onInitialize(ModContainer mod) {
		ServerLifecycleEvents.STARTING.register(this::onServerLoad);
		ServerLifecycleEvents.STOPPING.register(this::onServerStop);
		ScreenEvents.AFTER_INIT.register(MCWiFiPnP::afterScreenInit);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
		{
			DeOpCommands.register(dispatcher);
			OpCommand.register(dispatcher);
			WhitelistCommand.register(dispatcher);
			BanIpCommands.register(dispatcher);
			BanListCommands.register(dispatcher);
			BanPlayerCommands.register(dispatcher);
		});
	}

	public static void afterScreenInit(Screen screen, Minecraft client, int i, int j) {
		if (screen instanceof ShareToLanScreen) {
			client.setScreen(new ShareToLanScreenNew(screen));
		}
	}

	private void onServerLoad(MinecraftServer server) {
		MCWiFiPnPUnit.ReadingConfig(server);
	}

	private void onServerStop(MinecraftServer server) {
		MCWiFiPnPUnit.CloseUPnPPort(server);
	}

	public static void openToLan() {
		Minecraft client = Minecraft.getInstance();
		IntegratedServer server = client.getSingleplayerServer();
		PlayerList playerList = server.getPlayerList();
		MCWiFiPnPUnit.Config cfg = MCWiFiPnPUnit.getConfig(server);

		server.setMotd(cfg.motd);
		server.getStatus().setDescription(Component.literal(cfg.motd));
		MutableComponent component = server.publishServer(GameType.byName(cfg.GameMode), cfg.AllowCommands, cfg.port) ? PublishCommand.getSuccessMessage(cfg.port) : Component.translatable("commands.publish.failed");
		client.gui.getChat().addMessage(component);
		((PlayerListAccessor) playerList).setMaxPlayers(cfg.maxPlayers);
		server.setUsesAuthentication(cfg.OnlineMode);
		server.setPvpAllowed(cfg.PvP);
		server.setEnforceWhitelist(cfg.Whitelist);
		playerList.setUsingWhiteList(cfg.Whitelist);
		playerList.setAllowCheatsForAllPlayers(cfg.AllPlayersCheats);
		for (ServerPlayer player : playerList.getPlayers()) {
			playerList.sendPlayerPermissionLevel(player);
		}
		client.gui.getChat().addMessage(Component.translatable("commands.publish.started", cfg.port));

		new Thread(() -> {
			MCWiFiPnPUnit.UseUPnP(cfg, client);
			MCWiFiPnPUnit.CopyToClipboard(cfg, client);
		}, "MCWiFiPnP").start();
	}
}
