package mods.battlegear2.client;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import mods.battlegear2.Battlegear;
import mods.battlegear2.api.RenderItemBarEvent;
import mods.battlegear2.api.core.IBattlePlayer;
import mods.battlegear2.api.quiver.QuiverArrowRegistry;
import mods.battlegear2.client.gui.BattlegearInGameGUI;
import mods.battlegear2.client.gui.controls.GuiBGInventoryButton;
import mods.battlegear2.client.gui.controls.GuiPlaceableButton;
import mods.battlegear2.client.gui.controls.GuiSigilButton;
import mods.battlegear2.api.heraldry.PatternStore;
import mods.battlegear2.client.heraldry.CrestImages;
import mods.battlegear2.client.model.QuiverModel;
import mods.battlegear2.client.utils.BattlegearRenderHelper;
import mods.battlegear2.items.ItemQuiver;
import mods.battlegear2.packet.PickBlockPacket;
import mods.battlegear2.utils.BattlegearConfig;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSkeleton;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.opengl.GL11;

public class BattlegearClientEvents {

	private final BattlegearInGameGUI inGameGUI = new BattlegearInGameGUI();
	private final QuiverModel quiverModel = new QuiverModel();

	private final ResourceLocation quiverDetails = new ResourceLocation(
			"battlegear2", "textures/armours/quiver/QuiverDetails.png");
	private final ResourceLocation quiverBase = new ResourceLocation(
			"battlegear2", "textures/armours/quiver/QuiverBase.png");
    public static final ResourceLocation patterns = new ResourceLocation("battlegear2", "textures/heraldry/Patterns-small.png");
    public static int storageIndex;
	public static List<GuiPlaceableButton> tabsList = new ArrayList<GuiPlaceableButton>();
	static {
		tabsList.add(new GuiBGInventoryButton(0, 10, 10));
		tabsList.add(new GuiSigilButton(1, 20, 20));
	}

    @ForgeSubscribe(priority = EventPriority.HIGHEST)
    public void postRenderBar(RenderItemBarEvent.BattleSlots event) {
        if(!event.isMainHand){
            event.xOffset += BattlegearConfig.battleBarOffset[0];
            event.yOffset += BattlegearConfig.battleBarOffset[1];
        }else{
            event.xOffset += BattlegearConfig.battleBarOffset[2];
            event.yOffset += BattlegearConfig.battleBarOffset[3];
        }
    }

    @ForgeSubscribe(priority = EventPriority.HIGHEST)
    public void postRenderQuiver(RenderItemBarEvent.QuiverSlots event) {
        event.xOffset += BattlegearConfig.quiverBarOffset[0];
        event.yOffset += BattlegearConfig.quiverBarOffset[1];
    }

    @ForgeSubscribe(priority = EventPriority.HIGHEST)
    public void postRenderShield(RenderItemBarEvent.ShieldBar event) {
        event.xOffset += BattlegearConfig.shieldBarOffset[0];
        event.yOffset += BattlegearConfig.shieldBarOffset[1];
    }

	@ForgeSubscribe
	public void postRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
			inGameGUI.renderGameOverlay(event.partialTicks, event.mouseX, event.mouseY);
		}
	}

	@ForgeSubscribe
	public void render3rdPersonBattlemode(RenderPlayerEvent.Specials.Post event) {

		ModelBiped biped = (ModelBiped) event.renderer.mainModel;
		//getModelBiped(event.renderer, 1);
		BattlegearRenderHelper.renderItemIn3rdPerson(event.entityPlayer, biped,
				event.partialRenderTick);

		ItemStack mainhand = event.entityPlayer.getHeldItem();
		if (mainhand != null) {
			ItemStack quiverStack = QuiverArrowRegistry.getArrowContainer(
                    mainhand, event.entityPlayer);
			if (quiverStack != null
					&& quiverStack.getItem() instanceof ItemQuiver) {

				ItemQuiver quiver = (ItemQuiver) quiverStack.getItem();
				int col = quiver.getColor(quiverStack);
				float red = (float) (col >> 16 & 255) / 255.0F;
				float green = (float) (col >> 8 & 255) / 255.0F;
				float blue = (float) (col & 255) / 255.0F;
				int maxStack = quiver.getSlotCount(quiverStack);
				int arrowCount = 0;
				for (int i = 0; i < maxStack; i++) {
					arrowCount += quiver.getStackInSlot(quiverStack, i) == null ? 0
							: 1;
				}
				GL11.glColor3f(1, 1, 1);
				Minecraft.getMinecraft().renderEngine
						.bindTexture(quiverDetails);
				GL11.glPushMatrix();
				biped.bipedBody.postRender(0.0625F);
				GL11.glScalef(1.05F, 1.05F, 1.05F);
				quiverModel.render(arrowCount, 0.0625F);

				Minecraft.getMinecraft().renderEngine.bindTexture(quiverBase);
				GL11.glColor3f(red, green, blue);
				quiverModel.render(0, 0.0625F);
				GL11.glColor3f(1, 1, 1);

				GL11.glPopMatrix();
			}
		}
	}

    private static final int SKELETON_ARROW = 5;
	@ForgeSubscribe
	public void renderLiving(RenderLivingEvent.Post event) {

		if (BattlegearConfig.enableSkeletonQuiver && event.entity instanceof EntitySkeleton
				&& event.renderer instanceof RenderSkeleton) {
			//ObfuscationReflectionHelper.getPrivateValue(RenderBiped.class, (RenderBiped) event.renderer, 0);

			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_CULL_FACE);

			GL11.glColor3f(1, 1, 1);
			Minecraft.getMinecraft().renderEngine.bindTexture(quiverDetails);

			double d0 = (((EntitySkeleton) event.entity).lastTickPosX + ((((EntitySkeleton) event.entity).posX - ((EntitySkeleton) event.entity).lastTickPosX) * BattlegearClientTickHandeler.partialTick));
			double d1 = (((EntitySkeleton) event.entity).lastTickPosY + ((((EntitySkeleton) event.entity).posY - ((EntitySkeleton) event.entity).lastTickPosY) * BattlegearClientTickHandeler.partialTick));
			double d2 = (((EntitySkeleton) event.entity).lastTickPosZ + (((EntitySkeleton) event.entity).posZ - ((EntitySkeleton) event.entity).lastTickPosZ)
					* BattlegearClientTickHandeler.partialTick);

			GL11.glTranslatef((float) (d0 - RenderManager.renderPosX),
					(float) (d1 - RenderManager.renderPosY),
					(float) (d2 - RenderManager.renderPosZ));

			GL11.glScalef(1, -1, 1);

			float f2 = interpolateRotation(event.entity.prevRenderYawOffset,
					event.entity.renderYawOffset, 0);

			GL11.glRotatef(180.0F - f2, 0.0F, 1.0F, 0.0F);

			if (event.entity.deathTime > 0) {
				float f3 = ((float) event.entity.deathTime
						+ BattlegearClientTickHandeler.partialTick - 1.0F) / 20.0F * 1.6F;
				f3 = MathHelper.sqrt_float(f3);

				if (f3 > 1.0F) {
					f3 = 1.0F;
				}

				GL11.glRotatef(-f3 * 90, 0.0F, 0.0F, 1.0F);
			}

			GL11.glTranslatef(0, -1.5F, 0);

			GL11.glRotatef(event.entity.rotationPitch, 0, 1, 0);
            ((ModelBiped)event.renderer.mainModel).bipedBody.postRender(0.0625F);
			GL11.glScalef(1.05F, 1.05F, 1.05F);
			quiverModel.render(SKELETON_ARROW, 0.0625F);

			Minecraft.getMinecraft().renderEngine.bindTexture(quiverBase);
			GL11.glColor3f(0.10F, 0.10F, 0.10F);
			quiverModel.render(0, 0.0625F);
			GL11.glColor3f(1, 1, 1);

			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glPopMatrix();

		}

	}

    private static final int MAIN_INV = InventoryPlayer.getHotbarSize();
    @ForgeSubscribe(priority = EventPriority.HIGHEST)
    public void replacePickBlock(MouseEvent event){
        if(event.buttonstate && event.button-100==Minecraft.getMinecraft().gameSettings.keyBindPickBlock.keyCode){
            Minecraft mc = FMLClientHandler.instance().getClient();
            if(mc.thePlayer!=null && mc.theWorld!=null){
                event.setCanceled(true);
                if(!((IBattlePlayer)mc.thePlayer).isBattlemode()){
                    boolean isCreative = mc.thePlayer.capabilities.isCreativeMode;
                    ItemStack stack = getItemFromPointedAt(Minecraft.getMinecraft().objectMouseOver, mc.theWorld, isCreative);
                    if(stack!=null){
                        int k = -1;
                        ItemStack temp;
                        for(int slot=0; slot<MAIN_INV;slot++){
                            temp = mc.thePlayer.inventory.getStackInSlot(slot);
                            if(temp!=null && stack.isItemEqual(temp) && ItemStack.areItemStackTagsEqual(stack, temp)){
                                k = slot;
                                break;
                            }
                        }
                        if(isCreative && k==-1){
                            k = mc.thePlayer.inventory.getFirstEmptyStack();
                            if(k < 0 || k >= MAIN_INV){
                                k = mc.thePlayer.inventory.currentItem;
                            }
                        }
                        if (k >= 0 && k < MAIN_INV)
                        {
                            PacketDispatcher.sendPacketToServer(new PickBlockPacket(mc.thePlayer.username, stack, k).generatePacket());
                        }
                    }
                }
            }
        }
    }

    /**
     * Equivalent code to the creative pick block
     * @param target The client target vector
     * @param world The world of the player
     * @param creative If player is in creative mode
     * @return the stack expected for the creative pick button
     */
    private static ItemStack getItemFromPointedAt(MovingObjectPosition target, World world, boolean creative) {
        if(target!=null){
            if (target.typeOfHit == EnumMovingObjectType.TILE)
            {
                int x = target.blockX;
                int y = target.blockY;
                int z = target.blockZ;
                Block block = Block.blocksList[world.getBlockId(x, y, z)];
                if (block == null)
                {
                    return null;
                }
                return block.getPickBlock(target, world, x, y, z);
            }
            else
            {
                if (target.typeOfHit != EnumMovingObjectType.ENTITY || target.entityHit == null || !creative)
                {
                    return null;
                }
                return target.entityHit.getPickedResult(target);
            }
        }
        return null;
    }

	/**
	 * Returns a rotation angle that is inbetween two other rotation angles.
	 * par1 and par2 are the angles between which to interpolate, par3 is
	 * probably a float between 0.0 and 1.0 that tells us where "between" the
	 * two angles we are. Example: par1 = 30, par2 = 50, par3 = 0.5, then return
	 * = 40
	 */
	private float interpolateRotation(float par1, float par2, float par3) {
		float f3;

		for (f3 = par2 - par1; f3 < -180.0F; f3 += 360.0F) {
			;
		}

		while (f3 >= 180.0F) {
			f3 -= 360.0F;
		}

		return par1 + par3 * f3;
	}

	@ForgeSubscribe
	public void preStitch(TextureStitchEvent.Pre event) {
		if (event.map.textureType == 1) {
			ClientProxy.backgroundIcon = new Icon[2];
			ClientProxy.backgroundIcon[0] = event.map
					.registerIcon("battlegear2:slots/mainhand");
			ClientProxy.backgroundIcon[1] = event.map
					.registerIcon("battlegear2:slots/offhand");

			ClientProxy.bowIcons = new Icon[3];
			ClientProxy.bowIcons[0] = event.map
					.registerIcon("battlegear2:bow_pulling_0");
			ClientProxy.bowIcons[1] = event.map
					.registerIcon("battlegear2:bow_pulling_1");
			ClientProxy.bowIcons[2] = event.map
					.registerIcon("battlegear2:bow_pulling_2");

            storageIndex = PatternStore.DEFAULT.buildPatternAndStore(patterns);
            //CrestImages.initialise(Minecraft.getMinecraft().getResourceManager());
            /*for (HeraldryPattern pattern : HeraldryPattern.patterns) {
                pattern.registerIcon(event.map);
            }*/
		}
	}

	@ForgeSubscribe
	public void onSoundLoad(SoundLoadEvent event) {
		try {
			for (int i = 0; i < 10; i++) {
				event.manager.soundPoolSounds.addSound(String.format(
						"%s:%s%s.wav", "battlegear2", "shield", i));
			}

		} catch (Exception e) {
			Battlegear.logger.log(Level.WARNING, "Failed to register one or more sounds.");
		}
	}

	public static void onOpenGui(List buttons, int guiLeft, int guiTop) {
        if(BattlegearConfig.enableGuiButtons){
			int count = 0;
			for (GuiPlaceableButton button : tabsList) {
				button.place(count, guiLeft, guiTop);
				button.id = buttons.size()+2;//Due to GuiInventory and GuiContainerCreative button performed actions, without them having buttons...
				count++;
				buttons.add(button);
			}
        }
	}
}
