package pl.asie.computronics.integration.railcraft.tile;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import mods.railcraft.api.charge.IBatteryCart;
import mods.railcraft.common.carts.EntityLocomotiveElectric;
import mods.railcraft.common.items.ItemTicket;
import mods.railcraft.common.items.ItemTicketGold;
import mods.railcraft.common.plugins.forge.NBTPlugin;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.Optional;
import pl.asie.computronics.integration.railcraft.LocomotiveManager;
import pl.asie.computronics.reference.Config;
import pl.asie.computronics.reference.Mods;
import pl.asie.computronics.tile.TileEntityPeripheralBase;
import pl.asie.computronics.util.OCUtils;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Vexatos
 */
public class TileLocomotiveRelay extends TileEntityPeripheralBase implements ITickable {

	private WeakReference<EntityLocomotiveElectric> locomotive;
	//private boolean isInitialized = false;
	private boolean isBound = false;

	private UUID uuid;

	public TileLocomotiveRelay() {
		super("locomotive_relay", 0);
	}

	public void setLocomotive(EntityLocomotiveElectric loco) {
		this.locomotive = new WeakReference<EntityLocomotiveElectric>(loco);
		this.isBound = true;
		this.uuid = loco.getPersistentID();
	}

	@Nullable
	public EntityLocomotiveElectric getLocomotive() {
		return this.locomotive != null ? this.locomotive.get() : null;
	}

	public boolean isBound() {
		return this.isBound;
	}

	public boolean unbind() {
		if(this.isBound) {
			this.isBound = false;
			this.locomotive = null;
			this.uuid = null;
			return true;
		}
		return false;
	}

	@Override
	public void update() {
		super.update();

		if(world.isRemote) {
			return;
		}

		if(!isBound) {
			return;
		}

		if(uuid == null) {
			isBound = false;
		}

		if(uuid != null) {
			this.tryFindLocomotive(this.uuid);
		}
	}

	private void tryFindLocomotive(@Nullable UUID uuid) {
		if(uuid != null) {
			EntityLocomotiveElectric cart = LocomotiveManager.instance().getCartFromUUID(uuid);
			if(cart != null) {
				EntityLocomotiveElectric oldLoco = getLocomotive();
				if(oldLoco == null || cart != oldLoco) {
					this.setLocomotive(cart);
				}
			} else {
				this.locomotive = null;
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.isBound = nbt.getBoolean("bound");
		if(isBound) {
			this.uuid = NBTPlugin.readUUID(nbt, "locomotive");
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt = super.writeToNBT(nbt);
		if(isBound && this.uuid != null) {
			NBTPlugin.writeUUID(nbt, "locomotive", this.uuid);
		}
		nbt.setBoolean("bound", isBound);
		return nbt;
	}

	@Override
	public void removeFromNBTForTransfer(NBTTagCompound data) {
		super.removeFromNBTForTransfer(data);
		data.removeTag("locomotive");
		data.removeTag("bound");
	}

	@Override
	public NBTTagCompound writeToRemoteNBT(NBTTagCompound nbt) {
		nbt = super.writeToRemoteNBT(nbt);
		nbt.setBoolean("bound", isBound);
		return nbt;
	}

	@Override
	public void readFromRemoteNBT(NBTTagCompound nbt) {
		super.readFromRemoteNBT(nbt);
		if(nbt.hasKey("bound")) {
			this.isBound = nbt.getBoolean("bound");
		}
	}

	@Nullable
	private String cannotAccessLocomotive(double amount, boolean isOC) {
		EntityLocomotiveElectric locomotive = getLocomotive();
		if(!isBound) {
			return "relay is not bound to a locomotive";
		}
		if(locomotive == null) {
			return "locomotive is currently not detectable";
		}
		if(locomotive.dimension != this.world.provider.getDimension()) {
			return "relay and locomotive are in different dimensions";
		}
		if(locomotive.getDistanceSq(getPos()) > Config.LOCOMOTIVE_RELAY_RANGE * Config.LOCOMOTIVE_RELAY_RANGE) {
			return "locomotive is too far away";
		}
		if(locomotive.isSecure()) {
			return "locomotive is locked";
		}
		IBatteryCart cartBattery = LocomotiveManager.getCartBattery(locomotive);
		if(Config.LOCOMOTIVE_RELAY_CONSUME_CHARGE && cartBattery != null && (cartBattery.getCharge() <= 0
			|| cartBattery.removeCharge(10 * amount) < 10 * amount)) {
			return "locomotive out of energy";
		}
		if(isOC && Mods.isLoaded(Mods.OpenComputers)) {
			return cannotAccessLocomotive_OC(amount);
		}
		return null;
	}

	@Optional.Method(modid = Mods.OpenComputers)
	private boolean tryConsumeEnergy(double energy) {
		return !(node() instanceof Connector) || ((Connector) node()).tryChangeBuffer(-energy);
	}

	@Nullable
	@Optional.Method(modid = Mods.OpenComputers)
	private String cannotAccessLocomotive_OC(double amount) {
		if(!tryConsumeEnergy(Config.LOCOMOTIVE_RELAY_BASE_POWER * amount)) {
			return "not enough energy";
		}
		return null;
	}

	@Override
	@Optional.Method(modid = Mods.OpenComputers)
	protected OCUtils.Device deviceInfo() {
		return new OCUtils.Device(
			DeviceClass.Communication,
			"Locomotive interface",
			OCUtils.Vendors.Railcraft,
			"Locoremotive (TM)"
		);
	}

	//Computer stuff

	private static Object[] setDestination(EntityLocomotiveElectric locomotive, Object[] arguments) {
		ItemStack ticket = locomotive.getStackInSlot(0);
		if(ticket.getItem() instanceof ItemTicketGold) {
			ItemTicket.setTicketData(ticket, (String) arguments[0], (String) arguments[0],
				ItemTicketGold.getOwner(ticket));
			return new Object[] { locomotive.setDestination(ticket) };
		} else {
			return new Object[] { false, "there is no golden ticket inside the locomotive" };
		}
	}

	@Callback(doc = "function():string; gets the destination the locomotive is currently set to")
	@Optional.Method(modid = Mods.OpenComputers)
	public Object[] getDestination(Context context, Arguments args) {
		String error = cannotAccessLocomotive(1.0, true);
		if(error != null) {
			return new Object[] { null, error };
		}
		return new Object[] { getLocomotive().getDestination() };
	}

	@Callback(doc = "function(destination:string):boolean; Sets the locomotive's destination; there needs to be a golden ticket inside the locomotive")
	@Optional.Method(modid = Mods.OpenComputers)
	public Object[] setDestination(Context c, Arguments a) {
		String error = cannotAccessLocomotive(3.0, true);
		if(error != null) {
			return new Object[] { null, error };
		}
		a.checkString(0);
		return TileLocomotiveRelay.setDestination(getLocomotive(), a.toArray());
	}

	@Callback(doc = "function():number; gets the current charge of the locomotive")
	@Optional.Method(modid = Mods.OpenComputers)
	public Object[] getCharge(Context context, Arguments args) {
		String error = cannotAccessLocomotive(1.0, true);
		if(error != null) {
			return new Object[] { null, error };
		}
		IBatteryCart cartBattery = LocomotiveManager.getCartBattery(getLocomotive());
		return new Object[] { cartBattery != null ? cartBattery.getCharge() : 0.0D };
	}

	@Callback(doc = "function():string; returns the current mode of the locomotive; can be 'running' or 'shutdown'")
	@Optional.Method(modid = Mods.OpenComputers)
	public Object[] getMode(Context context, Arguments args) {
		String error = cannotAccessLocomotive(1.0, true);
		if(error != null) {
			return new Object[] { null, error };
		}
		return new Object[] { getLocomotive().getMode().toString().toLowerCase(Locale.ENGLISH) };
	}

	@Callback(doc = "function():string; returns the current name of the locomotive")
	@Optional.Method(modid = Mods.OpenComputers)
	public Object[] getName(Context context, Arguments args) {
		String error = cannotAccessLocomotive(1.0, true);
		if(error != null) {
			return new Object[] { null, error };
		}
		return new Object[] { getLocomotive().getName() };
	}

	@Override
	@Optional.Method(modid = Mods.ComputerCraft)
	public String[] getMethodNames() {
		return new String[] { "getDestination", "setDestination", "getCharge", "getMode", "getName" };
	}

	@Override
	@Optional.Method(modid = Mods.ComputerCraft)
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
		throws LuaException, InterruptedException {
		if(method < getMethodNames().length) {
			String error = cannotAccessLocomotive(method == 1 ? 3.0 : 1.0, false);
			if(error != null) {
				return new Object[] { null, error };
			}
			switch(method) {
				case 0: {
					return new Object[] { getLocomotive().getDestination() };
				}
				case 1: {
					if(arguments.length < 1 || !(arguments[0] instanceof String)) {
						throw new LuaException("first argument needs to be a string");
					}

					return TileLocomotiveRelay.setDestination(getLocomotive(), arguments);
				}
				case 2: {
					IBatteryCart cartBattery = LocomotiveManager.getCartBattery(getLocomotive());
					return new Object[] { cartBattery != null ? cartBattery.getCharge() : 0.0D };
				}
				case 3: {
					return new Object[] { getLocomotive().getMode().toString() };
				}
				case 4: {
					return new Object[] { getLocomotive().getName() };
				}
			}
		}
		return null;
	}
}
