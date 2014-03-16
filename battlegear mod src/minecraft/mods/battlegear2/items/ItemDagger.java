package mods.battlegear2.items;

import cpw.mods.fml.common.registry.GameRegistry;
import mods.battlegear2.Battlegear;
import mods.battlegear2.api.weapons.IBackStabbable;
import mods.battlegear2.api.weapons.IExtendedReachWeapon;
import mods.battlegear2.api.weapons.IHitTimeModifier;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntityDamageSource;

public class ItemDagger extends OneHandedWeapon implements IBackStabbable,IHitTimeModifier,IExtendedReachWeapon{

	public ItemDagger(int par1, EnumToolMaterial material, String name) {
		super(par1, material, name);
		//set the base damage to that of lower than usual (balance)
		this.baseDamage -= 2;
        GameRegistry.registerItem(this, this.name);
	}
	
	@Override
	public boolean canHarvestBlock(Block par1Block)//Daggers can harvest tallgrass and wool
    {
        return par1Block.blockID == Block.tallGrass.blockID||par1Block.blockID == Block.cloth.blockID;
    }

	@Override
	public int getHitTime(ItemStack stack,EntityLivingBase target) {
		return -5;
	}
	
	@Override//Here we simply cause more damage (hit will touch twice, one here and the other called vanilla)
	public boolean onBackStab(EntityLivingBase entityHit, EntityLivingBase entityHitting)
	{
        entityHit.attackEntityFrom(new EntityDamageSource(Battlegear.CUSTOM_DAMAGE_SOURCE, entityHitting), this.baseDamage/2);
        return true;
	}

    @Override
    public float getReachModifierInBlocks(ItemStack stack) {
        return -2;
    }
}
