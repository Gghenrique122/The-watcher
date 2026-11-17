package com.stalkerhorror.system;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.stalkerhorror.entity.StalkerStyle;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.CommandDispatcher;

/**
 * /stalker summon [estilo]  | /stalker haunt | /stalker sanity <0-100> | /stalker info | /stalker reset
 */
public final class StalkerCommands {
    private StalkerCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stalker")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("summon")
                        .executes(c -> summon(c, null))
                        .then(Commands.argument("estilo", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(StalkerStyle.values()).map(s -> s.name().toLowerCase()), b))
                                .executes(c -> summon(c, StringArgumentType.getString(c, "estilo")))))
                .then(Commands.literal("haunt").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Haunts.trigger(p, FearProfile.of(p));
                    return 1;
                }))
                .then(Commands.literal("sanity")
                        .then(Commands.argument("valor", FloatArgumentType.floatArg(0.0F, 100.0F))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    FearProfile.of(p).setSanity(FloatArgumentType.getFloat(c, "valor"));
                                    return 1;
                                })))
                .then(Commands.literal("reset").executes(c -> {
                    FearProfile.reset(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.literal("Memoria do Observador apagada."), false);
                    return 1;
                }))
                .then(Commands.literal("info").executes(StalkerCommands::info)));
    }

    private static int summon(CommandContext<CommandSourceStack> c, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        StalkerStyle st = name == null ? null : StalkerStyle.byName(name);
        if (name != null && st == null) {
            c.getSource().sendFailure(Component.literal("Estilo invalido. Use: watcher, peripheral, tunnel, pursuit, far, window"));
            return 0;
        }
        if (StalkerDirector.isActive(p)) {
            c.getSource().sendFailure(Component.literal("O Observador ja esta por perto."));
            return 0;
        }
        boolean ok = StalkerDirector.spawn(p, st, true);
        if (!ok) c.getSource().sendFailure(Component.literal("Nao achei um lugar escuro com linha de visao. Tente em outro lugar."));
        return ok ? 1 : 0;
    }

    private static int info(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        FearProfile f = FearProfile.of(p);
        String s = String.format(
                "§7[Observador] §fSanidade: %.0f/100 | Avistamentos: %d | Te encarou: %d | Voce fugiu: %d | Voce avancou: %d%n"
                        + "§7Habito dominante: §f%s §7| Dormiu: %d | Minerou: %d | Portas: %d | Baus: %d | Mortes: %d",
                f.sanity(), f.get("sightings"), f.get("stares"), f.get("flees"), f.get("approaches"),
                f.habit(), f.get("sleeps"), f.get("mining"), f.get("doors"), f.get("chests"), f.get("deaths"));
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }
}
