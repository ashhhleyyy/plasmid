package xyz.nucleoid.plasmid.command.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.manager.GameSpaceManager;
import xyz.nucleoid.plasmid.game.manager.ManagedGameSpace;
import xyz.nucleoid.plasmid.game.player.GamePlayerJoiner;
import xyz.nucleoid.plasmid.util.Guis;

import java.util.ArrayList;
import java.util.Comparator;

public class GameJoinUi extends SimpleGui {
    private static final GuiElementInterface EMPTY = new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(LiteralText.EMPTY.copy()).build();

    private static final int NAVBAR_POS = 81;
    private int tick;
    private int page = 0;
    private int pageSize;

    public GameJoinUi(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X6, player, true);
        this.setTitle(new TranslatableText("text.plasmid.ui.game_join.title"));
        this.updateUi();
    }

    private static void tryJoinGame(ServerPlayerEntity player, GameSpace gameSpace) {
        player.server.submit(() -> {
            var results = GamePlayerJoiner.tryJoin(player, gameSpace);
            results.sendErrorsTo(player);
        });
    }

    @Override
    public void onTick() {
        super.onTick();
        this.tick++;
        if (this.tick % 20 == 0) {
            this.updateUi();
        }
    }

    private void updateUi() {
        int i = 0;
        int gameI = 0;

        var games = new ArrayList<>(GameSpaceManager.get().getOpenGameSpaces());
        games.sort(Comparator.comparingInt(space -> -space.getPlayers().size()));

        int limit = this.size;
        this.pageSize = 0;

        if (games.size() > this.size) {
            limit = NAVBAR_POS;
            this.pageSize = games.size() / NAVBAR_POS;
        }

        this.page = MathHelper.clamp(this.page, 0, this.pageSize);

        for (ManagedGameSpace gameSpace : games) {
            if (gameI >= this.page * NAVBAR_POS) {
                if (i < limit) {
                    this.setSlot(i++, this.createIconFor(gameSpace));
                }
            }
            gameI++;
        }

        for (; i < limit; i++) {
            this.clearSlot(i);
        }

        if (this.pageSize != 0) {
            boolean hasPrevious = this.page != 0;
            boolean hasNext = this.page < this.pageSize;

            this.setSlot(NAVBAR_POS, EMPTY);
            this.setSlot(NAVBAR_POS + 1, EMPTY);

            this.setSlot(NAVBAR_POS + 2, new GuiElementBuilder(hasPrevious ? Items.LIME_STAINED_GLASS_PANE : Items.BLACK_STAINED_GLASS_PANE)
                    .setName(new TranslatableText("spectatorMenu.previous_page").formatted(hasPrevious ? Formatting.GOLD : Formatting.DARK_GRAY))
                    .setCallback((x, y, z) -> this.changePage(-1))
            );
            int pageValue = this.page + 1;

            this.setSlot(NAVBAR_POS + 3, Guis.getNumericBanner(pageValue / 100));
            this.setSlot(NAVBAR_POS + 4, Guis.getNumericBanner(pageValue / 10));
            this.setSlot(NAVBAR_POS + 5, Guis.getNumericBanner(pageValue));

            this.setSlot(NAVBAR_POS + 6, new GuiElementBuilder(hasNext ? Items.LIME_STAINED_GLASS_PANE : Items.BLACK_STAINED_GLASS_PANE)
                    .setName(new TranslatableText("spectatorMenu.next_page").formatted(hasNext ? Formatting.GOLD : Formatting.DARK_GRAY))
                    .setCallback((x, y, z) -> this.changePage(1))
            );

            this.setSlot(NAVBAR_POS + 7, EMPTY);
            this.setSlot(NAVBAR_POS + 8, EMPTY);
        }
    }

    private void changePage(int change) {
        this.page = MathHelper.clamp(this.page + change, 0, this.pageSize);
        this.updateUi();
    }

    private GuiElementBuilder createIconFor(GameSpace gameSpace) {
        var sourceConfig = gameSpace.getMetadata().sourceConfig();
        var element = GuiElementBuilder.from(sourceConfig.icon().copy())
                .setName(sourceConfig.name().shallowCopy());

        for (var line : sourceConfig.description()) {
            var text = line.shallowCopy();

            if (line.getStyle().getColor() == null) {
                text.setStyle(line.getStyle().withColor(Formatting.GRAY));
            }

            element.addLoreLine(text);
        }
        element.addLoreLine(LiteralText.EMPTY);
        element.addLoreLine(new LiteralText("")
                .append(new LiteralText("» ").formatted(Formatting.DARK_GRAY))
                .append(new TranslatableText("text.plasmid.ui.game_join.players",
                        new LiteralText(gameSpace.getPlayers().size() + "").formatted(Formatting.YELLOW)).formatted(Formatting.GOLD))
        );

        element.hideFlags();
        element.setCallback((a, b, c, d) -> tryJoinGame(this.getPlayer(), gameSpace));

        return element;
    }
}
