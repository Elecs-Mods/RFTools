package mcjty.rftools.commands;

import mcjty.rftools.playerprops.PlayerExtendedProperties;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CmdSetBuffBar extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "[<x> <y>]";
    }

    @Override
    public String getCommand() {
        return "buffs";
    }

    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length > 3) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Too many parameters!"));
            return;
        }

        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "This command only works as a player!"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        PlayerExtendedProperties properties = PlayerExtendedProperties.getProperties(player);

        if (args.length < 3) {
            int buffX = properties.getPreferencesProperties().getBuffX();
            int buffY = properties.getPreferencesProperties().getBuffY();
            ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Current buffbar location: " + buffX + "," + buffY));
            return;
        }

        int x = fetchInt(sender, args, 1, 0);
        int y = fetchInt(sender, args, 2, 0);
        properties.getPreferencesProperties().setBuffXY(x, y);
    }
}
