/**
 *  Thaumcraft Fix
 *  Copyright (c) 2024 TheCodex6824.
 *
 *  This file is part of Thaumcraft Fix.
 *
 *  Thaumcraft Fix is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Thaumcraft Fix is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Thaumcraft Fix.  If not, see <https://www.gnu.org/licenses/>.
 */

package thecodex6824.thaumcraftfix.core.transformer;

import java.util.function.Supplier;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.models.gear.ModelCustomArmor;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.EntityFluxRift;
import thecodex6824.coremodlib.FieldDefinition;
import thecodex6824.coremodlib.MethodDefinition;
import thecodex6824.coremodlib.PatchStateMachine;
import thecodex6824.thaumcraftfix.api.event.EntityInOuterLandsEvent;
import thecodex6824.thaumcraftfix.api.event.FluxRiftDestroyBlockEvent;
import thecodex6824.thaumcraftfix.core.transformer.custom.EntityAspectPrefixRemoverTransformer;
import thecodex6824.thaumcraftfix.core.transformer.custom.ThrowingTransformerWrapper;
import thecodex6824.thaumcraftfix.core.transformer.custom.TransformerBipedRotationCustomArmor;

public class EntityTransformers {

    public static final class HooksCommon {

	public static int isInOuterLands(int entityDim, Entity entity) {
	    EntityInOuterLandsEvent event = new EntityInOuterLandsEvent(entity);
	    MinecraftForge.EVENT_BUS.post(event);
	    boolean pass = event.getResult() == Result.ALLOW || (event.getResult() == Result.DEFAULT &&
		    entity.getEntityWorld().provider.getDimension() == ModConfig.CONFIG_WORLD.dimensionOuterId);
	    // if we want the check to pass, we return the entity dimension so the condition on TC's side passes
	    // otherwise, we pass a different dimension so the check will fail
	    return pass ? entityDim : entityDim + 1;
	}

	public static void clearDropChances(EntityLiving entity) {
	    for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
		entity.setDropChance(slot, 0.0f);
	    }
	}

	public static boolean isEntityDeadForProcessInteract(boolean original, EntityLivingBase entity) {
	    return original || entity.getHealth() <= 1.0e-5f;
	}

	public static boolean fireFluxRiftDestroyBlockEvent(EntityFluxRift rift, BlockPos pos, IBlockState state) {
	    return MinecraftForge.EVENT_BUS.post(new FluxRiftDestroyBlockEvent(rift, pos, state));
	}

    }

    @SideOnly(Side.CLIENT)
    public static final class HooksClient {

	public static void correctRotationPoints(ModelBiped model) {
	    if (model instanceof ModelCustomArmor) {
		if (model.isSneak) {
		    model.bipedRightLeg.rotationPointY = 13.0F;
		    model.bipedLeftLeg.rotationPointY = 13.0F;
		    model.bipedHead.rotationPointY = 4.5F;

		    model.bipedBody.rotationPointY = 4.5F;
		    model.bipedRightArm.rotationPointY = 5.0F;
		    model.bipedLeftArm.rotationPointY = 5.0F;
		}
		else {
		    model.bipedBody.rotationPointY = 0.0F;
		    model.bipedRightArm.rotationPointY = 2.0F;
		    model.bipedLeftArm.rotationPointY = 2.0F;
		}

		model.bipedHeadwear.rotationPointX = model.bipedHead.rotationPointX;
		model.bipedHeadwear.rotationPointY = model.bipedHead.rotationPointY;
		model.bipedHeadwear.rotationPointZ = model.bipedHead.rotationPointZ;
	    }
	}

	public static float getRobeRotationDivisor(Entity entity) {
	    float f = 1.0F;
	    if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getTicksElytraFlying() > 4) {
		f = (float) (entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ);
		f /= 0.2F;
		f = Math.max(f * f * f, 1.0F);
	    }

	    return f;
	}

    }

    private static final String HOOKS_COMMON = Type.getInternalName(HooksCommon.class);

    @SideOnly(Side.CLIENT)
    private static final String HOOKS_CLIENT = Type.getInternalName(HooksClient.class);

    // pretty much rewrites a model rotation method to not be incompatible with everything
    // this does not use ThrowingTransformerWrapper because it was intentionally designed to be exceptionally brittle
    // if it can't apply, it should not crash the game, and it will dump debug info itself
    public static final ITransformer CUSTOM_ARMOR_NOT_CALLING_SUPER = new TransformerBipedRotationCustomArmor();

    // compensates for the above transformer by adding a hook to set custom rotation points
    public static final ITransformer CUSTOM_ARMOR_ROTATION_POINTS = new GenericStateMachineTransformer(
	    PatchStateMachine.builder(
		    TransformUtil.remapMethod(new MethodDefinition(
			    "net/minecraft/client/model/ModelBiped",
			    false,
			    "func_78087_a",
			    Type.VOID_TYPE,
			    Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.FLOAT_TYPE,
			    Type.FLOAT_TYPE, Types.ENTITY
			    )
			    ))
	    .findNextLocalAccess(3)
	    .insertInstructions((node, matches) -> {
		InsnList toAdd = new InsnList();
		toAdd.add(new VarInsnNode(Opcodes.ALOAD, 0));
		toAdd.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
			HOOKS_CLIENT,
			"correctRotationPoints",
			Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType("Lnet/minecraft/client/model/ModelBiped;")),
			false
			));
		AbstractInsnNode match = matches.get(matches.size() - 1).matchStart();
		node.instructions.insertBefore(match.getPrevious().getPrevious().getPrevious(), toAdd);
		return ImmutableList.copyOf(toAdd.iterator());
	    })
	    .build(), true, 1 // important: just do this once
	    );

    // fixes annoying robe legging flapping (as if the player is walking) while elytra flying
    public static final ITransformer ELYTRA_ROBE_FLAPPING = new GenericStateMachineTransformer(
	    PatchStateMachine.builder(
		    TransformUtil.remapMethod(new MethodDefinition(
			    "thaumcraft/client/renderers/models/gear/ModelRobe",
			    false,
			    "func_78088_a",
			    Type.VOID_TYPE,
			    Types.ENTITY, Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.FLOAT_TYPE,
			    Type.FLOAT_TYPE, Type.FLOAT_TYPE
			    )
			    ))
	    .findNextMethodCall(new MethodDefinition(
		    "java/lang/Math",
		    false,
		    "min",
		    Type.FLOAT_TYPE,
		    Type.FLOAT_TYPE, Type.FLOAT_TYPE
		    ))
	    .insertInstructionsAfter(
		    new VarInsnNode(Opcodes.ALOAD, 1),
		    new MethodInsnNode(Opcodes.INVOKESTATIC,
			    HOOKS_CLIENT,
			    "getRobeRotationDivisor",
			    Type.getMethodDescriptor(Type.FLOAT_TYPE, Types.ENTITY),
			    false
			    ),
		    new InsnNode(Opcodes.FDIV)
		    )
	    .build(), true, 1
	    );

    // required because TC always creates fog near eldritch guardians if not in the outer lands
    // but since the outer lands don't exist they always do it
    // even if it did exist its hardcodedness is problematic
    public static final ITransformer ELDRITCH_GUARDIAN_FOG = new GenericStateMachineTransformer(
	    PatchStateMachine.builder(
		    TransformUtil.remapMethod(new MethodDefinition(
			    "thaumcraft/common/entities/monster/EntityEldritchGuardian",
			    false,
			    "func_70071_h_",
			    Type.VOID_TYPE
			    )
			    ))
	    .findConsecutive()
	    .findNextMethodCall(new MethodDefinition(
		    "net/minecraft/world/WorldProvider",
		    false,
		    "getDimension",
		    Type.INT_TYPE
		    ))
	    .findNextFieldAccess(new FieldDefinition(
		    "thaumcraft/common/config/ModConfig$CONFIG_WORLD",
		    "dimensionOuterId",
		    Type.INT_TYPE
		    ))
	    .endConsecutive()
	    .insertInstructionsAfter(new VarInsnNode(Opcodes.ALOAD, 0),
		    new MethodInsnNode(Opcodes.INVOKESTATIC,
			    HOOKS_COMMON,
			    "isInOuterLands",
			    Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE, Types.ENTITY),
			    false
			    )
		    )
	    .build()
	    );

    private static Supplier<ITransformer> makeEntityProcessInteractTransformer(String className) {
	return () -> {
	    return new GenericStateMachineTransformer(
		    PatchStateMachine.builder(
			    TransformUtil.remapMethod(new MethodDefinition(
				    className,
				    false,
				    "func_184645_a",
				    Type.BOOLEAN_TYPE,
				    Types.ENTITY_PLAYER, Type.getType("Lnet/minecraft/util/EnumHand;")
				    )
				    ))
		    .findNextFieldAccess(TransformUtil.remapField(new FieldDefinition(
			    className,
			    "field_70128_L",
			    Type.BOOLEAN_TYPE
			    )))
		    .insertInstructionsAfter(
			    new VarInsnNode(Opcodes.ALOAD, 0),
			    new MethodInsnNode(Opcodes.INVOKESTATIC,
				    HOOKS_COMMON,
				    "isEntityDeadForProcessInteract",
				    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE, Types.ENTITY_LIVING_BASE),
				    false
				    )
			    )
		    .build()
		    );
	};
    }

    public static final Supplier<ITransformer> ENTITY_ASPECTS = () ->
    new ThrowingTransformerWrapper(new EntityAspectPrefixRemoverTransformer());

    public static final Supplier<ITransformer> ADVANCED_CROSSBOW_PROCESS_INTERACT_DEAD =
	    makeEntityProcessInteractTransformer("thaumcraft/common/entities/construct/EntityTurretCrossbowAdvanced");

    public static final Supplier<ITransformer> BORE_PROCESS_INTERACT_DEAD =
	    makeEntityProcessInteractTransformer("thaumcraft/common/entities/construct/EntityArcaneBore");

    public static final Supplier<ITransformer> CROSSBOW_PROCESS_INTERACT_DEAD =
	    makeEntityProcessInteractTransformer("thaumcraft/common/entities/construct/EntityTurretCrossbow");

    public static final Supplier<ITransformer> GOLEM_PROCESS_INTERACT_DEAD =
	    makeEntityProcessInteractTransformer("thaumcraft/common/golems/EntityThaumcraftGolem");

    public static final Supplier<ITransformer> OWNED_CONSTRUCT_PROCESS_INTERACT_DEAD =
	    makeEntityProcessInteractTransformer("thaumcraft/common/entities/construct/EntityOwnedConstruct");

    public static final Supplier<ITransformer> OWNED_CONSTRUCT_ZERO_DROP_CHANCES = () -> {
	return new GenericStateMachineTransformer(
		PatchStateMachine.builder(
			new MethodDefinition(
				"thaumcraft/common/entities/construct/EntityOwnedConstruct",
				false,
				"<init>",
				Type.VOID_TYPE,
				Types.WORLD
				)
			)
		.findNextOpcode(Opcodes.RETURN)
		.insertInstructionsBefore(
			new VarInsnNode(Opcodes.ALOAD, 0),
			new MethodInsnNode(Opcodes.INVOKESTATIC,
				HOOKS_COMMON,
				"clearDropChances",
				Type.getMethodDescriptor(Type.VOID_TYPE, Types.ENTITY_LIVING),
				false
				)
			)
		.build()
		);
    };

    // fixes armor counting twice visually for void robe armor
    // I get a lot of reports/questions about it, so here it is
    public static final ITransformer VOID_ROBE_ARMOR_DISPLAY = new GenericStateMachineTransformer(
	    PatchStateMachine.builder(
		    new MethodDefinition(
			    "thaumcraft/common/items/armor/ItemVoidRobeArmor",
			    false,
			    "getArmorDisplay",
			    Type.INT_TYPE,
			    Types.ENTITY_PLAYER, Types.ITEM_STACK, Type.INT_TYPE
			    )
		    )
	    .findNextOpcode(Opcodes.IRETURN)
	    .insertInstructionsBefore(new InsnNode(Opcodes.ICONST_0))
	    .build()
	    );

    // to fire an event when a flux rift eats a block
    public static final ITransformer FLUX_RIFT_DESTROY_BLOCK_EVENT = new GenericStateMachineTransformer(
	    PatchStateMachine.builder(
		    TransformUtil.remapMethod(new MethodDefinition(
			    "thaumcraft/common/entities/EntityFluxRift",
			    false,
			    "func_70071_h_",
			    Type.VOID_TYPE
			    )
			    ))
	    .findConsecutive()
	    .findNextLocalAccess(0)
	    .findNextFieldAccess(TransformUtil.remapField(new FieldDefinition(
		    "thaumcraft/common/entities/EntityFluxRift",
		    "field_70170_p",
		    Types.WORLD
		    )))
	    .findNextLocalAccess(5)
	    .findNextMethodCall(TransformUtil.remapMethod(new MethodDefinition(
		    "net/minecraft/world/World",
		    false,
		    "func_175623_d",
		    Type.BOOLEAN_TYPE,
		    Types.BLOCK_POS
		    )))
	    .findNextInstructionType(JumpInsnNode.class)
	    .endConsecutive()
	    .insertInstructions((node, matches) -> {
		InsnList toAdd = new InsnList();
		toAdd.add(new VarInsnNode(Opcodes.ALOAD, 0));
		toAdd.add(new VarInsnNode(Opcodes.ALOAD, 5));
		toAdd.add(new VarInsnNode(Opcodes.ALOAD, 6));
		toAdd.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
			HOOKS_COMMON,
			"fireFluxRiftDestroyBlockEvent",
			Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType("Lthaumcraft/common/entities/EntityFluxRift;"),
				Types.BLOCK_POS, Types.I_BLOCK_STATE),
			false
			));
		JumpInsnNode originalJump = (JumpInsnNode) matches.get(0).matchEnd();
		toAdd.add(new JumpInsnNode(originalJump.getOpcode(), originalJump.label));

		ImmutableList<AbstractInsnNode> added = ImmutableList.copyOf(toAdd.iterator());
		node.instructions.insertBefore(matches.get(0).matchStart(), toAdd);

		return added;
	    })
	    .build()
	    );

}
