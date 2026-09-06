package net.lionarius.skinrestorer.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;

import java.util.concurrent.CompletableFuture;

// Extend server parsing while sending the ordinary quotable string type to vanilla clients.
// A greedy argument would consume invalid selectors and suppress their syntax errors.
final class UrlArgumentCommandNode<S> extends ArgumentCommandNode<S, String> {
    UrlArgumentCommandNode(ArgumentCommandNode<S, String> argument) {
        super(
                argument.getName(),
                argument.getType(),
                argument.getCommand(),
                argument.getRequirement(),
                argument.getRedirect(),
                argument.getRedirectModifier(),
                argument.isFork(),
                (context, builder) -> suggestQuotedUrl(argument, context, builder));
        argument.getChildren().forEach(this::addChild);
    }

    @Override
    public void parse(StringReader reader, CommandContextBuilder<S> context) throws CommandSyntaxException {
        if (!reader.canRead() || StringReader.isQuotedStringStart(reader.peek())) {
            super.parse(reader, context);
            return;
        }

        var start = reader.getCursor();
        var url = readUnquotedUrl(reader);
        context.withArgument(this.getName(), new ParsedArgument<>(start, reader.getCursor(), url));
        context.withNode(this, StringRange.between(start, reader.getCursor()));
    }

    private static <S> CompletableFuture<Suggestions> suggestQuotedUrl(
            ArgumentCommandNode<S, String> argument, CommandContext<S> context, SuggestionsBuilder builder)
            throws CommandSyntaxException {
        var reader = new StringReader(builder.getRemaining());
        if (reader.canRead() && !StringReader.isQuotedStringStart(reader.peek())) {
            var url = readUnquotedUrl(reader);
            var quoted = StringArgumentType.escapeIfRequired(url);
            if (!quoted.equals(url))
                return builder.suggest(quoted + reader.getRemaining()).buildFuture();
        }

        return argument.listSuggestions(context, builder);
    }

    private static String readUnquotedUrl(StringReader reader) {
        var start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) reader.skip();
        return reader.getString().substring(start, reader.getCursor());
    }
}
