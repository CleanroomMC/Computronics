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
import pl.asie.computronics.reference.Mods;

public class RadarTurtleUpgrade extends TurtleUpgradeBase {

	private static class RadarTurtlePeripheral extends TurtlePeripheralBase {

		public RadarTurtlePeripheral(ITurtleAccess access) {
			super(access);
		}

		@Override
		public String getType() {
			return "radar";
		}

		@Override
		@Optional.Method(modid = Mods.ComputerCraft)
		public String[] getMethodNames() {
			return CCRadarProxy.getMethodNames();
		}

		@Override
		@Optional.Method(modid = Mods.ComputerCraft)
		public Object[] callMethod(IComputerAccess computer,
			ILuaContext context, int method, Object[] arguments)
			throws LuaException, InterruptedException {
			return CCRadarProxy.callMethod(access.getWorld(), access.getPosition(),
				computer, context, method, arguments, access);
		}

	}

	public RadarTurtleUpgrade(String id) {
		super(id);
	}

	@Override
	public String getUnlocalisedAdjective() {
		return "Radar";
	}

	@Override
	public ItemStack getCraftingItem() {
		return new ItemStack(Computronics.radar, 1, 0);
	}

	@Override
	public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
		return new RadarTurtlePeripheral(turtle);
	}
}
