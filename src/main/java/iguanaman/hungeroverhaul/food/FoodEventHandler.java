package iguanaman.hungeroverhaul.food;

import net.minecraft.potion.PotionEffect;
import net.minecraft.world.EnumDifficulty;
import iguanaman.hungeroverhaul.HungerOverhaul;
import iguanaman.hungeroverhaul.IguanaConfig;
import squeek.applecore.api.food.FoodEvent;
import squeek.applecore.api.hunger.ExhaustionEvent;
import squeek.applecore.api.hunger.HealthRegenEvent;
import squeek.applecore.api.hunger.StarvationEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class FoodEventHandler
{
    @SubscribeEvent
    public void onFoodEaten(FoodEvent.FoodEaten event)
    {
        if (!event.player.worldObj.isRemote && event.player.worldObj.getGameRules().getGameRuleBooleanValue("naturalRegeneration") && IguanaConfig.healthRegenRatePercentage > 0)
        {
            if (IguanaConfig.addWellFedEffect)
            {
                int duration = (int) Math.pow(event.foodValues.hunger * 100, 1.2);

                if (duration >= 30)
                {
                    PotionEffect currentEffect = event.player.getActivePotionEffect(HungerOverhaul.potionWellFed);
                    if (currentEffect != null)
                        duration += currentEffect.getDuration();
                    event.player.addPotionEffect(new PotionEffect(HungerOverhaul.potionWellFed.id, duration, 0, true));
                }
            }
        }

        if (IguanaConfig.foodRegensHealth)
        {
            float toHeal = Math.round((float) event.foodValues.hunger / (float) IguanaConfig.foodHealDivider);
            float canHeal = event.player.getMaxHealth() - event.player.getHealth();
            if (toHeal > canHeal)
                toHeal = canHeal;
            if (toHeal > 0f)
                event.player.heal(toHeal);
        }
    }

    @SubscribeEvent
    public void onExhaustionTick(ExhaustionEvent.Tick event)
    {
        if (IguanaConfig.hungerLossRatePercentage == 0)
        {
            event.exhaustionLevel = 0f;
            event.player.getFoodStats().foodLevel = 19;
            event.player.getFoodStats().foodSaturationLevel = 0f;
            event.setCanceled(true);
            return;
        }

        EnumDifficulty difficulty = event.player.worldObj.difficultySetting;
        float hungerLossRate = 3f;
        if (IguanaConfig.difficultyScaling && IguanaConfig.difficultyScalingHunger)
        {
            if (difficulty == EnumDifficulty.PEACEFUL)
                hungerLossRate = 5F;
            else if (difficulty == EnumDifficulty.EASY)
                hungerLossRate = 4F;
        }
        event.maxExhaustionLevel = hungerLossRate / (IguanaConfig.hungerLossRatePercentage / 100F);
    }

    @SubscribeEvent
    public void onExhausted(ExhaustionEvent.MaxReached event)
    {
        // always decrease hunger, even in peaceful
        event.deltaHunger = -1;
    }

    @SubscribeEvent
    public void allowHealthRegen(HealthRegenEvent.Regen event)
    {
        if (event.player.getFoodStats().getFoodLevel() >= IguanaConfig.minHungerToHeal
                && IguanaConfig.healthRegenRatePercentage > 0
                && event.player.worldObj.getGameRules().getGameRuleBooleanValue("naturalRegeneration")
                && event.player.shouldHeal())
        {
            event.setResult(Result.ALLOW);
        }
        else
            event.setResult(Result.DENY);
    }

    @SubscribeEvent
    public void onHealthRegenTick(HealthRegenEvent.Tick event)
    {
        float wellfedModifier = 1.0F;
        if (event.player.isPotionActive(HungerOverhaul.potionWellFed))
            wellfedModifier = 0.75F;

        EnumDifficulty difficulty = event.player.worldObj.difficultySetting;
        float difficultyModifierHealing = 1.0F;
        if (IguanaConfig.difficultyScaling && IguanaConfig.difficultyScalingHealing)
        {
            if (difficulty.getDifficultyId() <= EnumDifficulty.EASY.getDifficultyId())
                difficultyModifierHealing = 0.75F;
            else if (difficulty == EnumDifficulty.HARD)
                difficultyModifierHealing = 1.5F;
        }

        float lowHealthModifier = event.player.getMaxHealth() - event.player.getHealth();
        lowHealthModifier *= IguanaConfig.lowHealthRegenRateModifier / 100F;
        lowHealthModifier *= difficultyModifierHealing;
        lowHealthModifier = (float) Math.pow(lowHealthModifier + 1F, 1.5F);

        event.regenTickPeriod = Math.round(80.0F * difficultyModifierHealing * wellfedModifier * lowHealthModifier
                / (IguanaConfig.healthRegenRatePercentage / 100F));
    }

    @SubscribeEvent
    public void onHealthRegen(HealthRegenEvent.Regen event)
    {
        if (IguanaConfig.disableHealingHungerDrain)
            event.deltaExhaustion = 0;
    }

    @SubscribeEvent
    public void onStarve(StarvationEvent.Starve event)
    {
        event.starveDamage = IguanaConfig.damageOnStarve;
    }

    @SubscribeEvent
    public void onFoodStatsAddition(FoodEvent.FoodStatsAddition event)
    {
        if (IguanaConfig.hungerLossRatePercentage == 0)
            event.setCanceled(true);
    }
}
