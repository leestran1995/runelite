/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Abexlry <abexlry@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.keyremapping;

import com.google.inject.Provides;
import java.awt.Color;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarClientInt;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.ColorUtil;

@PluginDescriptor(
	name = "Key Remapping",
	description = "Allows use of WASD keys for camera movement with 'Press Enter to Chat', and remapping number keys to F-keys",
	tags = {"enter", "chat", "wasd", "camera"},
	enabledByDefault = false
)
public class KeyRemappingPlugin extends Plugin
{
	private static final String PRESS_ENTER_TO_CHAT = "Press Enter to Chat...";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	@Inject
	private KeyRemappingListener inputListener;
	@Inject
	private MouseManager mouseManager;
	@Inject
	private MouseListener mouseListener;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private boolean typing;

	@Override
	protected void startUp() throws Exception
	{
		typing = false;
		keyManager.registerKeyListener(inputListener);
		mouseManager.registerMouseListener(mouseListener);

		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				lockChat();
				// Clear any typed text
				client.setVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT, "");
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				unlockChat();
			}
		});

		keyManager.unregisterKeyListener(inputListener);
	}

	@Provides
	KeyRemappingConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(KeyRemappingConfig.class);
	}

	boolean chatboxFocused()
	{
		Widget chatboxParent = client.getWidget(WidgetInfo.CHATBOX_PARENT);
		if (chatboxParent == null || chatboxParent.getOnKeyListener() == null)
		{
			return false;
		}

		// the search box on the world map can be focused, and chat input goes there, even
		// though the chatbox still has its key listener.
		Widget worldMapSearch = client.getWidget(WidgetInfo.WORLD_MAP_SEARCH);
		return worldMapSearch == null || client.getVarcIntValue(VarClientInt.WORLD_MAP_SEARCH_FOCUSED) != 1;
	}

	/**
	 * Check if a dialog is open that will grab numerical input, to prevent F-key remapping
	 * from triggering.
	 *
	 * @return
	 */
	boolean isDialogOpen()
	{
		// Most chat dialogs with numerical input are added without the chatbox or its key listener being removed,
		// so chatboxFocused() is true. The chatbox onkey script uses the following logic to ignore key presses,
		// so we will use it too to not remap F-keys.
		return isHidden(WidgetInfo.CHATBOX_MESSAGES) || isHidden(WidgetInfo.CHATBOX_TRANSPARENT_LINES)
			// We want to block F-key remapping in the bank pin interface too, so it does not interfere with the
			// Keyboard Bankpin feature of the Bank plugin
			|| !isHidden(WidgetInfo.BANK_PIN_CONTAINER);
	}

	boolean isOptionsDialogOpen()
	{
		return client.getWidget(WidgetInfo.DIALOG_OPTION) != null;
	}

	private boolean isHidden(WidgetInfo widgetInfo)
	{
		Widget w = client.getWidget(widgetInfo);
		return w == null || w.isSelfHidden();
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent scriptCallbackEvent)
	{
		switch (scriptCallbackEvent.getEventName())
		{
			case "setChatboxInput":
				Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
				if (chatboxInput != null && !typing)
				{
					setChatboxWidgetInput(chatboxInput, PRESS_ENTER_TO_CHAT);
				}
				break;
			case "blockChatInput":
				if (!typing)
				{
					int[] intStack = client.getIntStack();
					int intStackSize = client.getIntStackSize();
					intStack[intStackSize - 1] = 1;
				}
				break;
		}
	}

	void lockChat()
	{
		Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chatboxInput != null)
		{
			setChatboxWidgetInput(chatboxInput, PRESS_ENTER_TO_CHAT);
		}
	}

	void unlockChat()
	{
		Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chatboxInput != null)
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				final boolean isChatboxTransparent = client.isResized() && client.getVarbitValue(Varbits.TRANSPARENT_CHATBOX) == 1;
				final Color textColor = isChatboxTransparent ? JagexColors.CHAT_TYPED_TEXT_TRANSPARENT_BACKGROUND : JagexColors.CHAT_TYPED_TEXT_OPAQUE_BACKGROUND;
				setChatboxWidgetInput(chatboxInput, ColorUtil.wrapWithColorTag(client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT) + "*", textColor));
			}
		}
	}

	private void setChatboxWidgetInput(Widget widget, String input)
	{
		String text = widget.getText();
		int idx = text.indexOf(':');
		if (idx != -1)
		{
			String newText = text.substring(0, idx) + ": " + input;
			widget.setText(newText);
		}
	}
}
