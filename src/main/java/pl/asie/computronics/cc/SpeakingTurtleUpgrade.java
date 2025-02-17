package pl.asie.computronics.cc;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.reference.Config;
import pl.asie.computronics.reference.Mods;
import pl.asie.computronics.util.ChatBoxUtils;

public class SpeakingTurtleUpgrade extends TurtleUpgradeBase {

	private static class SpeakingTurtlePeripheral extends TurtlePeripheralBase {

		public SpeakingTurtlePeripheral(ITurtleAccess access) {
			super(access);
		}

		@Override
		public String getType() {
			return "chat_box";
		}

		@Override
		@Optional.Method(modid = Mods.ComputerCraft)
		public String[] getMethodNames() {
			return new String[] { "say" };
		}

		@Override
		@Optional.Method(modid = Mods.ComputerCraft)
		public Object[] callMethod(IComputerAccess computer,
			ILuaContext context, int method, Object[] arguments)
			throws LuaException, InterruptedException {
			if(arguments.length == 0 || !(arguments[0] instanceof String)) {
				return null;
			}

			int distance = Config.CHATBOX_DISTANCE;
			if(arguments.length > 1 && arguments[1] instanceof Double) {
				distance = Math.min(Config.CHATBOX_DISTANCE, ((Double) arguments[1]).intValue());
				if(distance <= 0) {
					distance = Config.CHATBOX_DISTANCE;
				}
			}
			String prefix = Config.CHATBOX_PREFIX;
			ChatBoxUtils.sendChatMessage(access.getWorld(), access.getPosition().getX(), access.getPosition().getY(), access.getPosition().getZ(),
				distance, prefix, ((String) arguments[0]));
			return null;
		}

	}

	public SpeakingTurtleUpgrade(String id) {
		super(id);
	}

	@Override
	public String getUnlocalisedAdjective() {
		return "Speaking";
	}

	@Override
	public ItemStack getCraftingItem() {
		return new ItemStack(Computronics.chatBox, 1, 0);
	}

	@Override
	public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
		return new SpeakingTurtlePeripheral(turtle);
	}
}
