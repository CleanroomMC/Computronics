package pl.asie.computronics.integration.buildcraft.statements.actions;

import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.asie.computronics.tile.TapeDriveState.State;
import pl.asie.computronics.util.StringUtil;

/**
 * @author Vexatos
 */
public enum Actions implements IActionExternal {
	Computer_Start("computer_start", new ActionComputer.Start()),
	Computer_Stop("computer_stop", new ActionComputer.Stop()),
	TapeDrive_Start("tape_drive_start", new ActionTapeDrive(State.PLAYING)),
	TapeDrive_Stop("tape_drive_stop", new ActionTapeDrive(State.STOPPED)),
	TapeDrive_Rewind("tape_drive_rewind", new ActionTapeDrive(State.REWINDING)),
	TapeDrive_Forward("tape_drive_forward", new ActionTapeDrive(State.FORWARDING)),
	Lamp_SetColor("lamp_color_set", new ActionLamp.Set(), 3, 3),
	Lamp_ResetColor("lamp_color_reset", new ActionLamp.Reset());

	public static final Actions[] VALUES = values();
	private String tag;
	private IComputronicsAction action;
	@SideOnly(Side.CLIENT)
	private TextureAtlasSprite icon;

	private int minParams = 0, maxParams = 0;

	Actions(String tag, IComputronicsAction action) {
		this.tag = tag;
		this.action = action;
	}

	Actions(String tag, IComputronicsAction action, int minParams, int maxParams) {
		this(tag, action);
		this.minParams = minParams;
		this.maxParams = maxParams;
	}

	public static void initialize() {
		for(Actions action : VALUES) {
			StatementManager.registerStatement(action);
		}
	}

	@Override
	public void actionActivate(TileEntity tile, EnumFacing side, IStatementContainer container, IStatementParameter[] parameters) {
		this.action.actionActivate(tile, side, container, parameters);
	}

	@Override
	public String getUniqueTag() {
		return "computroncis:action." + tag;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public TextureAtlasSprite getGuiSprite() {
		return this.icon;
	}

	@SideOnly(Side.CLIENT)
	public void stitchTextures(TextureStitchEvent.Pre event) {
		this.icon = event.map.registerSprite(new ResourceLocation("computronics", "items/buildcraft/actions/action." + this.tag));
	}

	@Override
	public int maxParameters() {
		return this.maxParams;
	}

	@Override
	public int minParameters() {
		return this.minParams;
	}

	@Override
	public IStatementParameter createParameter(int i) {
		if(this.action instanceof IComputronicsParameterAction) {
			return ((IComputronicsParameterAction) this.action).createParameter(i);
		}
		return null;
	}

	@Override
	public String getDescription() {
		return StringUtil.localize("tooltip.computronics.gate.action." + this.tag);
	}

	@Override
	public IStatement rotateLeft() {
		return this;
	}
}
