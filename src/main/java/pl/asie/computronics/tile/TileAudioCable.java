package pl.asie.computronics.tile;

import gnu.trove.set.hash.TIntHashSet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import pl.asie.computronics.api.audio.AudioPacket;
import pl.asie.computronics.api.audio.IAudioConnection;
import pl.asie.computronics.api.audio.IAudioReceiver;
import pl.asie.lib.tile.TileEntityBase;
import pl.asie.lib.util.ColorUtils;
import pl.asie.lib.util.internal.IColorable;

public class TileAudioCable extends TileEntityBase implements IAudioReceiver, IColorable, ITickable {

	private final TIntHashSet packetIds = new TIntHashSet();

	private int ImmibisMicroblocks_TransformableTileEntityMarker;

	private byte connectionMap = 0;
	private boolean initialConnect = false;

	private void updateConnections() {
		final byte oldConnections = connectionMap;
		connectionMap = 0;
		for(EnumFacing dir : EnumFacing.VALUES) {
			if(!connectsInternal(dir)) {
				continue;
			}

			if(worldObj.isBlockLoaded(getPos().offset(dir))) {
				TileEntity tile = worldObj.getTileEntity(getPos().offset(dir));
				if(tile instanceof TileAudioCable) {
					if(!((TileAudioCable) tile).connectsInternal(dir.getOpposite())) {
						continue;
					}
				} else if(tile instanceof IAudioConnection) {
					if(!((IAudioConnection) tile).connectsAudio(dir.getOpposite())) {
						continue;
					}
				} else {
					continue;
				}

				if(tile instanceof IColorable && ((IColorable) tile).canBeColored()
					&& !ColorUtils.isSameOrDefault(this, (IColorable) tile)) {
					continue;
				}

				connectionMap |= 1 << dir.ordinal();
			}
		}
		if(connectionMap != oldConnections) {
			worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
		}
	}

	protected boolean connectsInternal(EnumFacing dir) {
		return ImmibisMicroblocks_isSideOpen(dir.ordinal());
	}

	@Override
	public boolean connectsAudio(EnumFacing dir) {
		if(!initialConnect) {
			updateConnections();
			initialConnect = true;
		}
		return ((connectionMap >> dir.ordinal()) & 1) == 1;
	}

	@Override
	public void update() {
		packetIds.clear();
		updateConnections();
	}

	@Override
	public void receivePacket(AudioPacket packet, EnumFacing side) {
		if(packetIds.contains(packet.id)) {
			return;
		}

		packetIds.add(packet.id);
		for(EnumFacing dir : EnumFacing.VALUES) {
			if(dir == side) {
				continue;
			}

            if (connectsAudio(dir)) {
                if (!worldObj.isBlockLoaded(getPos().offset(dir))) {
                    continue;
                }

                TileEntity tile = worldObj.getTileEntity(getPos().offset(dir));
                if (tile instanceof IAudioReceiver) {
                    ((IAudioReceiver) tile).receivePacket(packet, dir.getOpposite());
                }
            }
		}
	}

	@Override
	public World getSoundWorld() {
		return null;
	}

	@Override
	public BlockPos getSoundPos() {
		return BlockPos.ORIGIN;
	}

	@Override
	public int getSoundDistance() {
		return 0;
	}

	protected int overlayColor = getDefaultColor();

	@Override
	public boolean canBeColored() {
		return true;
	}

	@Override
	public int getColor() {
		return overlayColor;
	}

	@Override
	public int getDefaultColor() {
		return ColorUtils.Color.LightGray.color;
	}

	@Override
	public void setColor(int color) {
		this.overlayColor = color;
		this.markDirty();
	}

	@Override
	public void readFromRemoteNBT(NBTTagCompound nbt) {
		super.readFromRemoteNBT(nbt);
		int oldColor = this.overlayColor;
		byte oldConnections = this.connectionMap;
		if(nbt.hasKey("col")) {
			overlayColor = nbt.getInteger("col");
		}
		if(this.overlayColor < 0) {
			this.overlayColor = getDefaultColor();
		}
		if(nbt.hasKey("con")) {
			this.connectionMap = nbt.getByte("con");
		}
		if(oldColor != this.overlayColor || oldConnections != this.connectionMap) {
			this.worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
		}
	}

	@Override
	public void writeToRemoteNBT(NBTTagCompound nbt) {
		super.writeToRemoteNBT(nbt);
		if(overlayColor != getDefaultColor()) {
			nbt.setInteger("col", overlayColor);
		}
		nbt.setByte("con", connectionMap);
	}

	@Override
	public void readFromNBT(final NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if(nbt.hasKey("col")) {
			overlayColor = nbt.getInteger("col");
		}
		if(this.overlayColor < 0) {
			this.overlayColor = getDefaultColor();
		}
		if(nbt.hasKey("con")) {
			this.connectionMap = nbt.getByte("con");
		}
	}

	@Override
	public void writeToNBT(final NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if(overlayColor != getDefaultColor()) {
			nbt.setInteger("col", overlayColor);
		}
		nbt.setByte("con", connectionMap);
	}

	public boolean ImmibisMicroblocks_isSideOpen(int side) {
		return true;
	}

	public void ImmibisMicroblocks_onMicroblocksChanged() {

	}
}
