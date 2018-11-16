
package org.jubaroo.mods.bountyonburn;

import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.MethodsItems;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BurnAllAction implements ModAction, ActionPerformer, BehaviourProvider {
    private ActionEntry actionEntry;
    private static Logger logger;

    static {
        BurnAllAction.logger = Logger.getLogger(BurnAllAction.class.getName());
    }

    BurnAllAction() {
        actionEntry = ActionEntry.createEntry((short) ModActions.getNextActionId(), "Burn all", "burning all", new int[]{
                6 /* ACTION_TYPE_NO_MOVE */,
                48 /* ACTION_TYPE_ENEMY_ALWAYS */,
                36 /* ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM */
        });
        ModActions.registerAction(actionEntry);
    }

    @Override
    public short getActionId() {
        return actionEntry.getNumber();
    }

    @Override
    public BehaviourProvider getBehaviourProvider() {
        return this;
    }

    @Override
    public ActionPerformer getActionPerformer() {
        return this;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item target) {
        if (Initiator.canUse(performer, source, target) || Initiator.canUseInForge(performer, source, target))
            return Collections.singletonList(actionEntry);
        else
            return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        try {
            Communicator comm = performer.getCommunicator();
            float x = performer.getPosX();
            float y = performer.getPosY();

            if (!performer.isWithinDistanceTo(target.getPosX(), target.getPosY(), performer.getPositionZ(), 4)) {
                comm.sendNormalServerMessage("You are too far away to burn the corpse.");
                action.stop(true);
                return true;
            }
            if (Initiator.isUniqueCorpse(source, target) && !Initiator.burnUniques) {
                comm.sendNormalServerMessage("This unique creature does not seem to be affected by the fire.");
                return true;
            }
            if (target.getName().contains("dragon") && !Initiator.burnDragons) {
                comm.sendNormalServerMessage("Dragon corpses are naturally immune to fire.");
                return true;
            }
            if (target.getTopParentOrNull().isVehicle()) {
                comm.sendNormalServerMessage(String.format("You can not burn the %s while it is in the vehicle.", target.getName()));
                return true;
            }
            if (!MethodsItems.isLootableBy(performer, target)) {
                comm.sendNormalServerMessage(String.format("You are not allowed to burn the %s.", target.getName()));
                return true;
            }
            if (Initiator.canUse(performer, source, target) || Initiator.canUseInForge(performer, source, target)) {
                if (counter == 1f) {
                    performer.getCurrentAction().setTimeLeft(Initiator.actionTime * 10);
                    performer.sendActionControl("burning", true, Initiator.actionTime * 10);
                    comm.sendNormalServerMessage(String.format("You touch the %s to the %s and it bursts into flames!", source.getName(), target.getName()));
                    Server.getInstance().broadCastAction(performer.getName() + " touches the " + source.getName() + " to the " + target.getName() + " and it bursts into flames!", performer, 5);
                    performer.playAnimation("place", false);
                    if (Initiator.magicFire) {
                        comm.sendAddEffect(target.getWurmId(), target.getWurmId(), (short) 27, target.getPosX(), target.getPosY(), target.getPosZ(), (byte) 0, "magicfire", Initiator.actionTime, 0f);
                    } else {
                        comm.sendAddEffect(target.getWurmId(), target.getWurmId(), (short) 27, target.getPosX(), target.getPosY(), target.getPosZ(), (byte) 0, "copperBrazierFire", Initiator.actionTime, 0f);
                    }
                } else {
                    if (counter * 10f > action.getTimeLeft()) {
                        if (source.getTemplateId() == ItemList.corpse) {
                            for (final Item i : source.getAllItems(false)) {
                                Items.destroyItem(i.getWurmId());
                            }
                            Items.destroyItem(source.getWurmId());
                        } else {
                            for (final Item i : target.getAllItems(false)) {
                                Items.destroyItem(i.getWurmId());
                            }
                            Items.destroyItem(target.getWurmId());
                        }
                        performer.addMoney(Initiator.coinBounty);
                        performer.modifyKarma(Initiator.karmaBounty);
                        comm.sendNormalServerMessage(Initiator.bountyMessage);
                        Initiator.jDebug(String.format("Player: %s performed BurnAllAction, on a %s, at Position X:%s, Y:%s", performer.getName(), target.getName(), x / 4, y / 4));
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "search action error", e);
            return true;
        }
    }

}


