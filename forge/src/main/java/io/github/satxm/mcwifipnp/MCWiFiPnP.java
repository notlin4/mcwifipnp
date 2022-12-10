package io.github.satxm.mcwifipnp;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
	public static final String MODID = "mcwifipnp";

	public MCWiFiPnP() {
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener(MCWiFiPnP::afterScreenInit);
	}

	public static void afterScreenInit(final ScreenEvent.Init.Post event) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = event.getScreen();
		if (screen instanceof ShareToLanScreen) {
			client.setScreen(new ShareToLanScreenNew(screen));
		}
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		MCWiFiPnPUnit.ReadingConfig(event.getServer());
		DeOpCommands.register(event.getServer().getCommands().getDispatcher());
		OpCommand.register(event.getServer().getCommands().getDispatcher());
		WhitelistCommand.register(event.getServer().getCommands().getDispatcher());
		BanIpCommands.register(event.getServer().getCommands().getDispatcher());
		BanListCommands.register(event.getServer().getCommands().getDispatcher());
		BanPlayerCommands.register(event.getServer().getCommands().getDispatcher());
	}

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		MCWiFiPnPUnit.CloseUPnPPort(event.getServer());
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
		server.getPlayerList().maxPlayers = cfg.maxPlayers;
		server.setUsesAuthentication(cfg.OnlineMode);
		server.setPvpAllowed(cfg.PvP);
		server.setEnforceWhitelist(cfg.Whitelist);
		playerList.setUsingWhiteList(cfg.Whitelist);
		playerList.setAllowCheatsForAllPlayers(cfg.AllPlayersCheats);
		for (ServerPlayer player : playerList.getPlayers()) {
			playerList.sendPlayerPermissionLevel(player);
		}

		new Thread(() -> {
			MCWiFiPnPUnit.UseUPnP(cfg, client);
			MCWiFiPnPUnit.CopyToClipboard(cfg, client);
		}, "MCWiFiPnP").start();
	}
}
