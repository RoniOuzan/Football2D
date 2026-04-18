package com.football.client;

import com.football.client.inputs.bot.InputBot;
import com.football.client.messages.Message;
import com.football.game.strategy.TeamStrategy;

public class BotClient extends Client {
    private final InputBot inputBot;

    public BotClient() {
        super(null);

        this.inputBot = new InputBot();
        this.inputs.add(this.inputBot);
    }

    @Override
    public void updateStrategy(TeamStrategy strategy) {
        this.inputBot.updateInput(strategy);
        super.updateStrategy(strategy);
    }

    @Override
    public void sendMessage(Message message) {}

    @Override
    public void sendMessage(String type, Object data) {}
}
