package mods.battlegear2.heraldry;

import cpw.mods.fml.common.network.PacketDispatcher;
import mods.battlegear2.api.heraldry.IFlagHolder;
import mods.battlegear2.api.heraldry.IHeraldryItem;
import mods.battlegear2.packet.BattlegearBannerPacket;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

import java.util.List;

/**
 * User: nerd-boy
 * Date: 2/08/13
 * Time: 11:53 AM
 *
 * Block class for a flag pole
 */
public class BlockFlagPole extends Block {

    private static final float[] woodTexDims = new float[5];
    private static final float[] ironTexDims = new float[5];
    static{
        woodTexDims[0] = 0F;
        woodTexDims[1] = 4F;
        woodTexDims[2] = 8F;
        woodTexDims[3] = 12F;
        woodTexDims[4] = 16F;

        ironTexDims[0] = 1F;
        ironTexDims[1] = 4.5F;
        ironTexDims[2] = 8F;
        ironTexDims[3] = 11.5F;
        ironTexDims[4] = 15;
    }

    public BlockFlagPole(int i) {
        super(i, Material.wood);
    }

    @Override
    public void getSubBlocks(int par1, CreativeTabs par2CreativeTabs, List par3List) {
        for(int i = 0; i < 5; i++){
            par3List.add(new ItemStack(par1, 1, i));
        }
    }

    @Override
    public int damageDropped(int par1){
        return par1 % 5;
    }

    @Override
    public void registerIcons(IconRegister par1IconRegister) {
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World par1World, int par2, int par3, int par4) {
        int side = getOrient(par1World.getBlockMetadata(par2, par3, par4));
        switch(side){
            case 0:
                return AxisAlignedBB.getAABBPool().getAABB((double)par2 + 6F/16F, (double)par3 + 0, (double)par4 + 6F/16F, (double)par2 + 10F/16F, (double)par3 + 1, (double)par4 + 10F/16F);
            case 1:
                return AxisAlignedBB.getAABBPool().getAABB((double)par2 + 6F/16F, (double)par3 + 13F/16F, (double)par4 + 0, (double)par2 + 10F/16F, (double)par3 + 1, (double)par4 + 1);
            case 2:
                return AxisAlignedBB.getAABBPool().getAABB((double)par2 + 0, (double)par3 + 13F/16F, (double)par4 + 6F/16F, (double)par2 + 1, (double)par3 + 1, (double)par4 + 10F/16F);
        }
        return super.getSelectedBoundingBoxFromPool(par1World, par2, par3, par4);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4) {
        return getSelectedBoundingBoxFromPool(par1World, par2, par3, par4);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9) {

        TileEntity te = world.getBlockTileEntity(x, y, z);
        if(te != null && te instanceof IFlagHolder){
            ItemStack stack = par5EntityPlayer.getCurrentEquippedItem();
            if(stack == null){

                if(!world.isRemote){
                    List<ItemStack> flags = ((IFlagHolder) te).getFlags();
                    if(flags.size()>0){
                        ItemStack flag = flags.remove(flags.size() - 1);
                        par5EntityPlayer.inventory.setInventorySlotContents(par5EntityPlayer.inventory.currentItem, flag);
                        PacketDispatcher.sendPacketToAllInDimension(new BattlegearBannerPacket(x, y, z, flags).generatePacket(), world.provider.dimensionId);
                    }
                }

                return true;
            }else if(stack.getItem() instanceof IHeraldryItem){

                if(!world.isRemote){
                    if(((IFlagHolder) te).addFlag(stack)){
                        if(!par5EntityPlayer.capabilities.isCreativeMode){
                            par5EntityPlayer.inventory.decrStackSize(par5EntityPlayer.inventory.currentItem, 1);
                        }
                        PacketDispatcher.sendPacketToAllInDimension(new BattlegearBannerPacket(x, y, z, ((IFlagHolder) te).getFlags()).generatePacket(), world.provider.dimensionId);
                    }
                }
                return true;
            }
        }
        return super.onBlockActivated(world, x, y, z, par5EntityPlayer, par6, par7, par8, par9);
    }

    public float getTextDim(int metadata, int section){
        if(metadata % 5 == 4){
            return ironTexDims[section];
        }else{
            return woodTexDims[section];
        }
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean hasTileEntity(int i){
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int i) {
        return new TileEntityFlagPole();
    }

    @Override
    public Icon getIcon(int par1, int par2) {

        if(par2 % 5 == 4)
            return Block.blockIron.getIcon(par1,0);
        else{
            return Block.wood.getIcon(par1,par2 % 5);
        }
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta)
    {
        meta = meta % 5 + (5 * (side / 2));
        return meta;
    }

    public int getOrient(int meta){
        return meta / 5;
    }

    @Override
    public void breakBlock(World par1World, int par2, int par3, int par4, int par5, int par6) {
        if(!par1World.isRemote){
            TileEntity te = par1World.getBlockTileEntity(par2, par3, par4);
            if(te != null && te instanceof IFlagHolder){
                List<ItemStack> flags = ((IFlagHolder)te).getFlags();

                for(ItemStack f : flags){
                    par1World.spawnEntityInWorld(new EntityItem(par1World, par2, par3, par4, f));
                }
            }
        }
        super.breakBlock(par1World, par2, par3, par4, par5, par6);
    }
}
