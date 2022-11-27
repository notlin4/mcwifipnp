package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
		MinecraftForge.EVENT_BUS.addListener(this::ChangeButton);
	}

	@SubscribeEvent
	public void ChangeButton(ScreenEvent.InitScreenEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = event.getScreen();
		if (screen instanceof ShareToLanScreen) {
			client.setScreen(new ShareToLanScreenNew(screen));
		}
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		MCWiFiPnPUnit.ReadingConfig(event.getServer());
	}

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		MCWiFiPnPUnit.ClosePortUPnP(event.getServer());
	}

	public static void openToLan() {
		Minecraft client = Minecraft.getInstance();
		IntegratedServer server = client.getSingleplayerServer();
		MCWiFiPnPUnit.Config cfg = MCWiFiPnPUnit.getConfig(server);

		server.setMotd(cfg.motd);
		server.getStatus().setDescription(new TextComponent(cfg.motd));
		server.publishServer(GameType.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		server.getPlayerList().maxPlayers = cfg.maxPlayers;
		server.setUsesAuthentication(cfg.OnlineMode);
		server.setPvpAllowed(cfg.PvP);
		client.gui.getChat().addMessage(new TranslatableComponent("commands.publish.started", cfg.port));

		new Thread(() -> {
			MCWiFiPnPUnit.UseUPnP(cfg, client);
			MCWiFiPnPUnit.CopyToClipboard(cfg, client);
		}, "MCWiFiPnP").start();
	}
}
