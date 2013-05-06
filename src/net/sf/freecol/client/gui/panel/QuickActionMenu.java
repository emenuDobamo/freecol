/**
 *  Copyright (C) 2002-2012   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ColonyPanel.TilesPanel.ASingleTilePanel;
import net.sf.freecol.client.gui.panel.UnitLabel.UnitAction;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;


/**
 * Handles the generation of popup menu's generated by
 * draglistner objects attached to units within the
 * Colony and Europe panels.
 * @author Brian
 */
public final class QuickActionMenu extends JPopupMenu {

    private static final Logger logger = Logger.getLogger(QuickActionMenu.class.getName());

    private final FreeColPanel parentPanel;

    private FreeColClient freeColClient;

    private GUI gui;

    /**
     * Creates a standard empty menu
     */
    public QuickActionMenu(FreeColClient freeColClient, GUI gui, FreeColPanel freeColPanel)
    {
        this.freeColClient = freeColClient;
        this.gui = gui;
        this.parentPanel = freeColPanel;
    }

    /**
     * Creates a popup menu for a Unit.
     */
    public void createUnitMenu(final UnitLabel unitLabel) {
        ImageLibrary imageLibrary = parentPanel.getLibrary();
        final Unit tempUnit = unitLabel.getUnit();
        this.setLabel("Unit");
        ImageIcon unitIcon = imageLibrary.getUnitImageIcon(tempUnit, 0.66);

        JMenuItem name = new JMenuItem(Messages.message(tempUnit.getLabel()) + " (" +
                                       Messages.message("menuBar.colopedia") + ")",
                                       unitIcon);
        name.setActionCommand(UnitAction.COLOPEDIA.toString());
        name.addActionListener(unitLabel);
        this.add(name);
        this.addSeparator();

        if (tempUnit.isCarrier()) {
            if (addCarrierItems(unitLabel)) {
                this.addSeparator();
            }
        }

        if (tempUnit.getLocation().getTile() != null) {
            Colony colony = tempUnit.getLocation().getTile().getColony();
            if (colony != null) {
            	if (addTileItem(unitLabel)) {
                    this.addSeparator();
                }
            	if (addWorkItems(unitLabel)) {
                    this.addSeparator();
                }
                if (addEducationItems(unitLabel)) {
                    this.addSeparator();
                }
                if (tempUnit.getLocation() instanceof WorkLocation
                    && colony.canReducePopulation()) {
                    JMenuItem menuItem = new JMenuItem(Messages.message("leaveTown"));
                    menuItem.setActionCommand(UnitAction.LEAVE_TOWN.toString());
                    menuItem.addActionListener(unitLabel);
                    this.add(menuItem);
                    addBoardItems(unitLabel, colony.getTile());
                    this.addSeparator();
                } else {
                    if (addCommandItems(unitLabel)) {
                        this.addSeparator();
                    }
                    if (addBoardItems(unitLabel, colony.getTile())) {
                        this.addSeparator();
                    }
                }
            } else {
                if (addCommandItems(unitLabel)) {
                    this.addSeparator();
                }
            }
        } else if (tempUnit.isInEurope()) {
            if (addCommandItems(unitLabel)) {
                this.addSeparator();
            }
            if (addBoardItems(unitLabel, tempUnit.getOwner().getEurope())) {
                this.addSeparator();
            }
        }
        if (tempUnit.hasAbility(Ability.CAN_BE_EQUIPPED)) {
            if (addEquipmentItems(unitLabel)) {
                this.addSeparator();
            }
        }
    }

    private boolean addBoardItems(final UnitLabel unitLabel, Location loc) {
        final Unit tempUnit = unitLabel.getUnit();

        if (tempUnit.isCarrier()) return false;
        boolean added = false;
        for (Unit unit : loc.getUnitList()) {
            if (unit.isCarrier() && unit.canCarryUnits()
                && unit.canAdd(tempUnit)
                && tempUnit.getLocation() != unit) {
                final Unit funit = unit;
                final InGameController igc = freeColClient.getInGameController();
                StringTemplate template = StringTemplate.template("board")
                    .addStringTemplate("%unit%", unit.getLabel());
                JMenuItem menuItem = new JMenuItem(Messages.message(template));
                menuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            igc.boardShip(tempUnit, funit);
                        }
                    });
                this.add(menuItem);
                added = true;
            }
        }
        return added;
    }

    private boolean addLoadItems(final GoodsLabel goodsLabel, Location loc) {
        final InGameController igc = freeColClient.getInGameController();
        final Goods goods = goodsLabel.getGoods();

        boolean added = false;
        for (Unit unit : loc.getUnitList()) {
            if (unit.isCarrier() && unit.canCarryGoods()
                && unit.canAdd(goods)) {
                final Unit funit = unit;
                StringTemplate template = StringTemplate.template("loadOnTo")
                    .addStringTemplate("%unit%", unit.getLabel());
                JMenuItem menuItem = new JMenuItem(Messages.message(template));
                menuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            igc.loadCargo(goods, funit);
                        }
                    });
                this.add(menuItem);
                added = true;
            }
        }
        return added;
    }

    private boolean addCarrierItems(final UnitLabel unitLabel) {
        final Unit tempUnit = unitLabel.getUnit();

        if (tempUnit.hasCargo()) {
            JMenuItem cargo = new JMenuItem(Messages.message("cargoOnCarrier"));
            this.add(cargo);

            for (Unit passenger : tempUnit.getUnitList()) {
                JMenuItem menuItem = new JMenuItem("    " + Messages.message(passenger.getLabel()));
                menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
                this.add(menuItem);
            }
            for (Goods goods : tempUnit.getGoodsList()) {
                JMenuItem menuItem = new JMenuItem("    " + Messages.message(goods.getLabel(true)));
                menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
                this.add(menuItem);
            }
            return true;
        } else {
            return false;
        }
    }

    private List<JMenuItem> descendingList(final Map<JMenuItem, Integer> map) {
        List<JMenuItem> ret = new ArrayList<JMenuItem>(map.keySet());
        Collections.sort(ret, new Comparator<JMenuItem>() {
                public int compare(JMenuItem m1, JMenuItem m2) {
                    Integer i1 = map.get(m1);
                    Integer i2 = map.get(m2);
                    int cmp = i2.compareTo(i1);
                    if (cmp == 0) cmp = m1.getText().compareTo(m2.getText());
                    return cmp;
                }
            });
        return ret;
    }

    private JMenuItem makeProductionItem(GoodsType type, WorkLocation wl,
                                         int amount, UnitLabel unitLabel,
                                         boolean claim) {
        StringTemplate t = StringTemplate.template(type.getId() + ".workAs")
            .addAmount("%amount%", amount);
        if (claim) {
            t.addStringTemplate("%claim%", wl.getClaimTemplate());
        } else {
            t.addName("%claim%", "");
        }
        JMenuItem menuItem = new JMenuItem(Messages.message(t),
            parentPanel.getLibrary().getScaledGoodsImageIcon(type, 0.66f));
        menuItem.setActionCommand(UnitLabel.getWorkLabel(wl)
            + "/" + wl.getId() + "/" + type.getId()
            + "/" + ((claim) ? "!" : ""));
        menuItem.addActionListener(unitLabel);
        return menuItem;
    }

    private boolean addWorkItems(final UnitLabel unitLabel) {
        final Unit unit = unitLabel.getUnit();
        final UnitType unitType = unit.getType();
        final GoodsType expertGoods = unitType.getExpertProduction();
        final Colony colony = unit.getLocation().getColony();
        final Specification spec = freeColClient.getGame().getSpecification();
        WorkLocation current = (unit.getLocation() instanceof WorkLocation)
            ? (WorkLocation)unit.getLocation() : null;
        final int bonusChange = (current != null) ? 0
            : colony.governmentChange(colony.getWorkLocationUnitCount() + 1);

        Map<JMenuItem, Integer> items = new HashMap<JMenuItem, Integer>();
        Map<JMenuItem, Integer> extras = new HashMap<JMenuItem, Integer>();
        JMenuItem expertOwned = null;
        JMenuItem expertUnowned = null;
        for (GoodsType type : spec.getGoodsTypeList()) {
            int bestOwnedProd = 0;
            int bestUnownedProd = 0;
            WorkLocation bestOwned = null;
            WorkLocation bestUnowned = null;
            for (WorkLocation wl : colony.getAllWorkLocations()) {
                int prod = bonusChange;
                switch (wl.getNoAddReason(unit)) {
                case NONE:
                    prod += wl.getPotentialProduction(type, unitType);
                    if (prod > bestOwnedProd) {
                        bestOwnedProd = prod;
                        bestOwned = wl;
                    }
                    break;
                case ALREADY_PRESENT:
                    prod += wl.getPotentialProduction(type, unitType);
                    if (prod > bestOwnedProd) {
                        bestOwnedProd = prod;
                        bestOwned = (unit.getWorkType() == type) ? null : wl;
                    }
                    break;
                case CLAIM_REQUIRED:
                    prod += wl.getPotentialProduction(type, unitType);
                    if (prod > bestUnownedProd) {
                        bestUnownedProd = prod;
                        bestUnowned = wl;
                    }
                    break;
                default:
                    break;
                }
            }
            if (bestOwned != null) {
                JMenuItem ji = makeProductionItem(type, bestOwned,
                    bestOwnedProd, unitLabel, false);
                if (type == expertGoods) {
                    expertOwned = ji;
                } else {
                    items.put(ji, new Integer(bestOwnedProd));
                }
            }
            if (bestUnowned != null && bestUnownedProd > bestOwnedProd) {
                JMenuItem ji = makeProductionItem(type, bestUnowned,
                    bestUnownedProd, unitLabel, true);
                if (type == expertGoods) {
                    expertUnowned = ji;
                } else {
                    extras.put(ji, new Integer(bestUnownedProd));
                }
            }
        }

        JMenu container = new JMenu(Messages.message("model.unit.changeWork"));
        List<JMenuItem> owned = descendingList(items);
        if (expertOwned != null) owned.add(0, expertOwned);
        for (JMenuItem j : owned) container.add(j);
        List<JMenuItem> unowned = descendingList(extras);
        if (expertUnowned != null) unowned.add(0, expertUnowned);
        if (!unowned.isEmpty()) {
            if (!owned.isEmpty()) container.addSeparator();
            for (JMenuItem j : unowned) container.add(j);
        }
        if (container.getItemCount() > 0) this.add(container);

        if (current != null) {
            JMenuItem ji = new JMenuItem(Messages.message("showProductivity"));
            ji.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        gui.showWorkProductionPanel(unit);
                    }
                });
            this.add(ji);
        }
        return !(owned.isEmpty() && unowned.isEmpty() && current == null);
    }

    private boolean addEducationItems(final UnitLabel unitLabel) {
        boolean separatorNeeded = false;
        Unit unit = unitLabel.getUnit();
        ImageLibrary imageLibrary = parentPanel.getLibrary();

        if (unit.getSpecification().getBoolean(GameOptions.ALLOW_STUDENT_SELECTION)) {
            for (Unit teacher : unit.getColony().getTeachers()) {
                if (unit.canBeStudent(teacher) && (unit.getLocation() instanceof WorkLocation)) {
                    JMenuItem menuItem = null;
                    ImageIcon teacherIcon = imageLibrary.getUnitImageIcon(teacher, 0.5);
                    if (teacher.getStudent() != unit) {
                        String assign = Messages.message("assignToTeacher");
                        if (teacher.getStudent() != null) {
                            assign += " (" + teacher.getTurnsOfTraining()
                                + "/" + teacher.getNeededTurnsOfTraining()
                                + ")";
                        }
                        menuItem = new JMenuItem(assign, teacherIcon);
                        menuItem.setActionCommand(UnitAction.ASSIGN.toString() + "/" + teacher.getId());
                        menuItem.addActionListener(unitLabel);
                    } else {
                        String teacherName = Messages.message(teacher.getType().getNameKey());
                        menuItem = new JMenuItem(Messages.message(StringTemplate
                                .template("menu.unit.apprentice")
                                    .addName("%unit%", teacherName))
                            + ": " + teacher.getTurnsOfTraining()
                            + "/" + teacher.getNeededTurnsOfTraining(),
                            teacherIcon);
                        menuItem.setEnabled(false);
                    }
                    this.add(menuItem);
                    separatorNeeded = true;
                }
            }
        }
        if (unit.getStudent() != null) {
            Unit student = unit.getStudent();
            String studentName = Messages.message(student.getType().getNameKey());
            JMenuItem menuItem = new JMenuItem(Messages.message(StringTemplate
                    .template("menuBar.teacher")
                        .addName("%unit%", studentName))
                + ": " + unit.getTurnsOfTraining()
                + "/" + unit.getNeededTurnsOfTraining());
            menuItem.setEnabled(false);
            this.add(menuItem);
            separatorNeeded = true;
        }
        int experience = unit.getExperience();
        GoodsType goods = unit.getExperienceType();
        if (experience > 0 && goods != null) {
            UnitType expertType = freeColClient.getGame().getSpecification().getExpertForProducing(goods);
            if (unit.getType().canBeUpgraded(expertType, ChangeType.EXPERIENCE)) {
                int maxExperience = unit.getType().getMaximumExperience();
                double probability = unit.getType().getUnitTypeChange(expertType)
                    .getProbability(ChangeType.EXPERIENCE) * experience / (double) maxExperience;
                String jobName = Messages.message(goods.getWorkingAsKey());
                ImageIcon expertIcon = imageLibrary.getUnitImageIcon(expertType, 0.5);
                JMenuItem experienceItem = new JMenuItem(Messages.message(StringTemplate.template("menu.unit.experience")
                                                                          .addName("%job%", jobName))
                                                         + " " + experience + "/" + maxExperience + " ("
                                                         + FreeColPanel.getModifierFormat().format(probability) + "%)",
                                                         expertIcon);
                experienceItem.setEnabled(false);
                this.add(experienceItem);
                separatorNeeded = true;
            }
        }
        return separatorNeeded;
    }


    private boolean addCommandItems(final UnitLabel unitLabel) {
        final Unit tempUnit = unitLabel.getUnit();
        final boolean isUnitAtSea = tempUnit.isAtSea();

        JMenuItem menuItem = new JMenuItem(Messages.message("activateUnit"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (tempUnit.getState() != Unit.UnitState.ACTIVE) {
                        freeColClient.getInGameController()
                            .changeState(tempUnit, Unit.UnitState.ACTIVE);
                    }
                    gui.setActiveUnit(tempUnit);
                }
            });
        menuItem.setEnabled(!isUnitAtSea);
        this.add(menuItem);

        if (!(tempUnit.getLocation() instanceof Europe)) {
            menuItem = new JMenuItem(Messages.message("fortifyUnit"));
            menuItem.setActionCommand(UnitAction.FORTIFY.toString());
            menuItem.addActionListener(unitLabel);
            menuItem.setEnabled((tempUnit.getMovesLeft() > 0)
                && !(tempUnit.getState() == Unit.UnitState.FORTIFIED
                    || tempUnit.getState() == Unit.UnitState.FORTIFYING));
            this.add(menuItem);
        }

        UnitState unitState = tempUnit.getState();
        menuItem = new JMenuItem(Messages.message("sentryUnit"));
        menuItem.setActionCommand(UnitAction.SENTRY.toString());
        menuItem.addActionListener(unitLabel);
        menuItem.setEnabled(unitState != Unit.UnitState.SENTRY
            && !isUnitAtSea);
        this.add(menuItem);

        boolean hasTradeRoute = tempUnit.getTradeRoute() != null;
        menuItem = new JMenuItem(Messages.message("clearUnitOrders"));
        menuItem.setActionCommand(UnitAction.CLEAR_ORDERS.toString());
        menuItem.addActionListener(unitLabel);
        menuItem.setEnabled((unitState != Unit.UnitState.ACTIVE
                || hasTradeRoute)
            && !isUnitAtSea);
        this.add(menuItem);

        if (tempUnit.isCarrier()) {
            menuItem = new JMenuItem(Messages.message("assignTradeRoute"));
            menuItem.setActionCommand(UnitAction.ASSIGN_TRADE_ROUTE.toString());
            menuItem.addActionListener(unitLabel);
            menuItem.setEnabled(!hasTradeRoute);
            this.add(menuItem);
        }

        if (tempUnit.canCarryTreasure() && tempUnit.canCashInTreasureTrain()) {
            menuItem = new JMenuItem(Messages.message("cashInTreasureTrain.order"));
            menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        freeColClient.getInGameController()
                            .checkCashInTreasureTrain(tempUnit);
                    }
                });
            this.add(menuItem);
        }

        if (tempUnit.getLocation() instanceof Unit) {
            menuItem = new JMenuItem(Messages.message("leaveShip"));
            menuItem.setActionCommand(UnitAction.LEAVE_SHIP.toString());
            menuItem.addActionListener(unitLabel);
            menuItem.setEnabled(true);
            this.add(menuItem);
        }

        if (tempUnit.isCarrier()) {
            menuItem = new JMenuItem(Messages.message("unload"));
            menuItem.setActionCommand(UnitAction.UNLOAD.toString());
            menuItem.addActionListener(unitLabel);
            menuItem.setEnabled(tempUnit.hasCargo() && !isUnitAtSea);
            this.add(menuItem);
        }

        return true;
    }

    private boolean addEquipmentItems(final UnitLabel unitLabel) {
        final Unit tempUnit = unitLabel.getUnit();
        final InGameController igc = freeColClient.getInGameController();
        ImageLibrary imageLibrary = parentPanel.getLibrary();
        boolean separatorNeeded = false;
        if (tempUnit.getEquipment().size() > 1) {
            JMenuItem newItem = new JMenuItem(Messages.message("model.equipment.removeAll"));
            newItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Map<EquipmentType, Integer> equipment =
                            new HashMap<EquipmentType, Integer>(tempUnit.getEquipment().getValues());
                        for (Map.Entry<EquipmentType, Integer> entry: equipment.entrySet()) {
                            igc.equipUnit(tempUnit, entry.getKey(), -entry.getValue());
                        }
                        unitLabel.updateIcon();
                    }
                });
            this.add(newItem);
        }

        EquipmentType horses = null;
        EquipmentType muskets = null;
        for (EquipmentType equipmentType : freeColClient.getGame().getSpecification().getEquipmentTypeList()) {
            int count = tempUnit.getEquipment().getCount(equipmentType);
            if (count > 0) {
                // "remove current equipment" action
                JMenuItem newItem = new JMenuItem(Messages.message(equipmentType.getId() + ".remove"));
                if (equipmentType.needsGoodsToBuild()) {
                    GoodsType goodsType = equipmentType.getRequiredGoods().get(0).getType();
                    newItem.setIcon(imageLibrary.getScaledGoodsImageIcon(goodsType, 0.66f));
                }
                final int items = count;
                final EquipmentType type = equipmentType;
                newItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            igc.equipUnit(tempUnit, type, -items);
                            unitLabel.updateIcon();
                        }
                    });
                this.add(newItem);
            }
            if (tempUnit.canBeEquippedWith(equipmentType) && count == 0) {
                // "add new equipment" action
                JMenuItem newItem = null;
                count = equipmentType.getMaximumCount() - count;
                if (!equipmentType.needsGoodsToBuild()) {
                    newItem = new JMenuItem();
                    newItem.setText(Messages.message(equipmentType.getId() + ".add"));
                } else if (tempUnit.isInEurope() &&
                           tempUnit.getOwner().getEurope().canBuildEquipment(equipmentType)) {
                    int price = 0;
                    newItem = new JMenuItem();
                    for (AbstractGoods ag : equipmentType.getRequiredGoods()) {
                        price += tempUnit.getOwner().getMarket().getBidPrice(ag.getType(),
                                                                             ag.getAmount());
                        newItem.setIcon(imageLibrary.getScaledGoodsImageIcon(ag.getType(), 0.66f));
                    }
                    while (!tempUnit.getOwner().checkGold(count * price)) {
                        count--;
                    }
                    newItem.setText(Messages.message(equipmentType.getId() + ".add") + " (" +
                                    Messages.message(StringTemplate.template("goldAmount")
                                                     .addAmount("%amount%", count * price)) +
                                    ")");
                } else if (tempUnit.getColony() != null &&
                           tempUnit.getColony().canBuildEquipment(equipmentType)) {
                    newItem = new JMenuItem();
                    for (AbstractGoods ag : equipmentType.getRequiredGoods()) {
                        int present = tempUnit.getColony()
                            .getGoodsCount(ag.getType()) / ag.getAmount();
                        if (present < count) {
                            count = present;
                        }
                        newItem.setIcon(imageLibrary.getScaledGoodsImageIcon(ag.getType(), 0.66f));
                    }
                    newItem.setText(Messages.message(equipmentType.getId() + ".add"));
                }
                if (newItem != null) {
                    // for convenience menu only
                    if ("model.equipment.horses".equals(equipmentType.getId())) {
                        horses = equipmentType;
                    } else if ("model.equipment.muskets".equals(equipmentType.getId())) {
                        muskets = equipmentType;
                    }
                    final int items = count;
                    final EquipmentType type = equipmentType;
                    newItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                igc.equipUnit(tempUnit, type, items);
                                unitLabel.updateIcon();
                            }
                        });
                    this.add(newItem);
                }
            }
        }

        // convenience menu for equipping dragoons
        if (horses != null && muskets != null && horses.isCompatibleWith(muskets)) {
            final EquipmentType horseType = horses;
            final EquipmentType musketType = muskets;
            JMenuItem newItem = new JMenuItem(Messages.message("model.equipment.dragoon"),
                imageLibrary.getUnitImageIcon(tempUnit.getType(), Role.DRAGOON, 1.0/3));
            newItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        igc.equipUnit(tempUnit, horseType, 1);
                        igc.equipUnit(tempUnit, musketType, 1);
                        unitLabel.updateIcon();
                    }
                });
            this.add(newItem);
        }

        separatorNeeded = true;

        if (separatorNeeded) {
            this.addSeparator();
            separatorNeeded = false;
        }

        UnitType newUnitType = tempUnit.getType().getTargetType(ChangeType.CLEAR_SKILL, tempUnit.getOwner());
        if (newUnitType != null) {
            JMenuItem menuItem = new JMenuItem(Messages.message("clearSpeciality"),
                imageLibrary.getUnitImageIcon(newUnitType, 1.0/3));
            menuItem.setActionCommand(UnitAction.CLEAR_SPECIALITY.toString());
            menuItem.addActionListener(unitLabel);
            this.add(menuItem);
            if(tempUnit.getLocation() instanceof Building &&
               !((Building)tempUnit.getLocation()).canAddType(newUnitType)){
                    menuItem.setEnabled(false);
            }
            separatorNeeded = true;
        }
        return separatorNeeded;
    }

    /**
     * Creates a menu for a tile.
     */
    public void createTileMenu(final ASingleTilePanel singleTilePanel) {
        if (singleTilePanel.getColonyTile() != null && singleTilePanel.getColonyTile().getColony() != null) {
            addTileItem(singleTilePanel.getColonyTile().getWorkTile());
        }
    }

    private boolean addTileItem(final UnitLabel unitLabel) {
        final Unit unit = unitLabel.getUnit();
        if (unit.getWorkTile() != null) {
            final Tile tile = unit.getWorkTile().getWorkTile();
            addTileItem(tile);
           return true;
        }
        return false;
    }

    private void addTileItem(final Tile tile) {
        if (tile != null) {
            JMenuItem menuItem = new JMenuItem(Messages.message(tile.getNameKey()));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    gui.showTilePanel(tile);
                }
            });
            add(menuItem);
        }
    }

    /**
     * Creates a menu for a good.
     */
    public void createGoodsMenu(final GoodsLabel goodsLabel) {
        final InGameController igc = freeColClient.getInGameController();
        final Player player = freeColClient.getMyPlayer();
        final Goods goods = goodsLabel.getGoods();
        ImageLibrary imageLibrary = parentPanel.getLibrary();
        this.setLabel("Cargo");
        JMenuItem name = new JMenuItem(Messages.message(goods.getNameKey()) + " (" +
                                       Messages.message("menuBar.colopedia") + ")",
                                       imageLibrary.getScaledGoodsImageIcon(goods.getType(), 0.66f));
        name.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.showColopediaPanel(goods.getType().getId());
                }
            });
        this.add(name);

        if (goods.getLocation() instanceof Colony) {
            Colony colony = (Colony)goods.getLocation();
            addLoadItems(goodsLabel, colony.getTile());

        } else if (goods.getLocation() instanceof Europe) {
            ; // add purchase items?

        } else if (goods.getLocation() instanceof Unit) {
            Unit carrier = (Unit)goods.getLocation();

            if (carrier.getLocation().getColony() != null
                || (carrier.isInEurope()
                    && player.canTrade(goods.getType()))) {
                JMenuItem unload = new JMenuItem(Messages.message("unload"));
                unload.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            igc.unloadCargo(goods, false);
                        }
                    });
                this.add(unload);
            } else {
                if (carrier.isInEurope()
                    && !player.canTrade(goods.getType())) {
                    JMenuItem pay = new JMenuItem(Messages.message("boycottedGoods.payArrears"));
                    pay.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                igc.payArrears(goods.getType());
                                // TODO fix pcls so this hackery can go away
                                if (parentPanel instanceof CargoPanel) {
                                    CargoPanel cargoPanel = (CargoPanel) parentPanel;
                                    cargoPanel.initialize();
                                }
                                parentPanel.revalidate();
                            }
                        });
                    this.add(pay);
                }
                JMenuItem dump = new JMenuItem(Messages.message("dumpCargo"));
                dump.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            igc.unloadCargo(goods, true);
                            // TODO fix pcls so this hackery can go away
                            if (parentPanel instanceof CargoPanel) {
                                ((CargoPanel) parentPanel).initialize();
                            }
                            parentPanel.revalidate();
                        }
                    });
                this.add(dump);
            }
        }
    }
}
