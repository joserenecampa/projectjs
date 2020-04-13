var IDENTIFIER = uuidv4();
var timeSheetDayOffset = 0;
let uri = window.location.href.split('?');
let VARS = {};
if (uri.length == 2) {
    let vars = uri[1].split('&');
    let tmp = '';
    vars.forEach(function(v){
        tmp = v.split('=');
        if(tmp.length == 2) {
            VARS[tmp[0]] = tmp[1];
        }   
    });
}
var modalIdGroup = '';
var app = new Vue({
    el: '#app',
    data: {
        form: {
            nome: '',
            grupo: 'Wipedown',
            moverPara: '',
            hora: '',
            minuto: '',
            entrada: 0
        },
        employeeName: '',
        employeeNote: '',
        daySheet: [],
        items: []
    },
    methods: {
        orderGroupsByLastWork: function (event) {
            orderGroupsByLastWork();
        },
        orderGroupsBySumWork: function (event) {
            orderGroupsBySumWork();
        },
        adicionar: function (event) {
            adicionar(this.form.nome, this.form.grupo);
        },
        preencherTimeSheet: function (event, idSubGroup) {
            this.items = [];
            var subGroup = groups.get(idSubGroup);
            if (subGroup) {
                this.employeeName = subGroup.employeeName;
                var resultItems = [];
                var total = 0;
                items.forEach(function (i) {
                    if (i.group == subGroup.id) {
                        if (i) {
                            var item = {};
                            var h = 0;
                            var m = 0;
                            if (i.type && i.type == "background") {
                                item.type = "Scheduled";
                            } else if (i.typeOfWork) {
                                item.type = i.typeOfWork;
                            }
                            item.start = (i.start.hours() < 10 ? "0" + i.start.hours() : i.start.hours()) + ':' + (i.start.minutes() < 10 ? "0" + i.start.minutes() : i.start.minutes());
                            if (i.end) {
                                item.end = (i.end.hours() < 10 ? "0" + i.end.hours() : i.end.hours()) + ':' + (i.end.minutes() < 10 ? "0" + i.end.minutes() : i.end.minutes());
                                var h = (i.end.diff(i.start, 'hours') % 24);
                                var m = (i.end.diff(i.start, 'minutes') % 60);
                                if (item.type != "Scheduled" && item.type != "Lunch" && item.type != "Push") {
                                    total = total + (h * 60) + m;
                                }
                            } else {
                                item.end = '';
                            }
                            item.id = i.id;
                            item.open = i.open;
                            item.selecionado = false;
                            item.elapsedTime = "" + (h < 10 && h >= 0 ? "0" + h : h) + ":" + (m < 10 && m >= 0 ? "0" + m : m);
                            resultItems.push(item);
                        }
                    }
                });
                resultItems.forEach(function (item) {
                    if (item.type === 'Home') {
                        minute = Math.floor(total % 60);
                        hours = Math.floor((total / 60) % 24);
                        item.elapsedTime = (hours < 10 && hours >= 0 ? "0" + hours : hours) + ":" + (minute < 10 && minute >= 0 ? "0" + minute : minute);
                    }
                });
                this.items = resultItems;
            }
        },
        atualizarSheets: function (idSubGroup, event) {
            this.items.forEach(function (i) {
                var item = items.get(i.id);
                item.typeOfWork = i.type;
                switch (item.typeOfWork) {
                    case 'Push': item.className = 'aquamarine'; break;
                    case 'Work': item.className = 'green'; break;
                    case 'Break': item.className = 'red'; break;
                    case 'Lunch': item.className = 'orange'; break;
                }
                var hora = i.start.split(":")[0];
                var minuto = i.start.split(":")[1];
                item.start = getMoment().hours(hora).minutes(minuto).seconds(0).milliseconds(0);
                if (item.typeOfWork != "Home") {
                    hora = i.end.split(":")[0];
                    minuto = i.end.split(":")[1];
                    item.end = getMoment().hours(hora).minutes(minuto).seconds(0).milliseconds(0);
                }
                items.update(item);
                persistItem(item, 'I');
            });
            this.preencherTimeSheet(event, idSubGroup);
        },
        removerSheets: function (idSubGroup, event) {
            this.items.forEach(function (i) {
                if (i.selecionado) {
                    removerAvisoVisual(idSubGroup);
                    items.remove(i.id);
                    removeItem(i, 'I');
                }
            });
            this.items = this.items.filter(function (v) { return !v.selecionado; });
        },
        mover: function (event) {
            var subGrupos = groups.get({
                filter: function (item) {
                    return (item.checked);
                }
            });
            var moverTo = getGroupByName(this.form.moverPara);
            if (moverTo.length == 0) {
                moverTo = groups.add({ content: this.form.moverPara, nestedGroups: [] })[0];
            } else {
                moverTo = moverTo[0];
            }
            subGrupos.forEach(function (gr) {
                var parentGroup = groups.get(gr.nestedInGroup);
                parentGroup.nestedGroups = parentGroup.nestedGroups.filter(function (v) { return v != gr.id; });
                groups.update(parentGroup);
                gr.nestedInGroup = moverTo.id;
                groups.update(gr);
                moverTo.nestedGroups.push(gr.id);
                groups.update(moverTo);
                gr.checked = false;
                checkme(gr, gr.id);
                persistItem(parentGroup, 'G');
                persistItem(gr, 'G');
                persistItem(moverTo, 'G');
            });
        },
        apagarSelecionados: function (event) {
            var subGrupos = groups.get({
                filter: function (item) {
                    return (item.checked);
                }
            });
            subGrupos.forEach(function (gr) {
                items.forEach(function (item) {
                    if (item.group === gr.id) {
                        items.remove(item);
                        removeItem(item, 'I');
                    }
                });
                var parentGroup = groups.get(gr.nestedInGroup);
                if (typeof parentGroup.nestedGroups != 'undefined' && parentGroup.nestedGroups.length > 0) {
                    parentGroup.nestedGroups = parentGroup.nestedGroups.filter(function (v) { return v != gr.id; });
                }
                groups.update(parentGroup);
                groups.remove(gr.id);
                persistItem(parentGroup, 'G');
                removeItem(gr, 'G');
            });
        }
    }
})


var items = new vis.DataSet([]);
var groups = new vis.DataSet([]);

var timeline = null;
var container = document.getElementById("TimeLine");
var customTimeHour = moment();
timeline = new vis.Timeline(container);
timeline.setGroups(groups);
timeline.setItems(items);
timeline.on('currentTimeTick', actionFired);
timeline.on('click', showModal);
function updateOptions() {
    var options = {
        stack: false,
        orientation: { axis: 'both' },
        maxHeight: 490,
        min: getMoment().hours(7).minutes(0).seconds(0).milliseconds(0),
        max: getMoment().hours(21).minutes(0).seconds(0).milliseconds(0),
        start: getMoment().hours(7).minutes(0).seconds(0).milliseconds(0),
        end: getMoment().hours(21).minutes(0).seconds(0).milliseconds(0),
        zoomMin: 1000 * 60 * 60 * 14,
        zoomMax: 1000 * 60 * 60 * 14,
        snap: function (date, scale, step) {
            const hour = 60 * 1000;
            return Math.round(date / hour) * hour;
        }
    };
    timeline.setOptions(options);
    timeline.fit();
}
updateOptions();
makeATestGrid();
function addOffSet() {
    timeSheetDayOffset = timeSheetDayOffset + 1;
    updateOptions();
    makeATestGrid();
    load();
}
function subOffSet() {
    timeSheetDayOffset = timeSheetDayOffset - 1;
    updateOptions();
    makeATestGrid();
    load();
}
function getMoment() {
    if (VARS && VARS.test && VARS.test==='OK') {
        return moment(customTimeHour);
    } else {
        return moment().add(timeSheetDayOffset, 'days');
    }
}
function makeATestGrid() {
    if (VARS && VARS.test && VARS.test==='OK') {
        timeline.off('currentTimeTick', actionFired);
        customTimeHour = getMoment().hours(10).minutes(0).seconds(0).milliseconds(0);
        actionFired();
        timeline.addCustomTime(customTimeHour, 'customTimeToTest');
        timeline.on('timechange', function (properties) {
            customTimeHour = properties.time;
            actionFired(properties);
        });
    }
}
function orderGroupsBySumWork() {
    groups.forEach(function (group) {
        group.order = 0;
        groups.update(group);
    });
    items.forEach(function (item) {
        if (item.typeOfWork === 'Work') {
            var group = groups.get(item.group);
            group.order = group.order - item.end.diff(item.start);
            groups.update(group);
        }
    });
    items.forEach(function (item) {
        if (item.typeOfWork === 'Home') {
            var group = groups.get(item.group);
            group.order = 0;
            groups.update(group);
        }
    });
}
function orderGroupsByLastWork() {
    groups.forEach(function (group) {
        group.order = 0;
        groups.update(group);
    });
    items.forEach(function (item) {
        if (item.typeOfWork === 'Work' && item.open) {
            var group = groups.get(item.group);
            group.order = group.order - item.end.diff(item.start);
            groups.update(group);
        }
    });
}
function showModal(properties) {
    if (properties && properties.group && properties.what == 'group-label') {
        var group = groups.get(properties.group);
        if (group && group.employeeName) {
            modalIdGroup = group.id;
            var objectCanBreak = canBreak(modalIdGroup);
            //app.employeeNote = objectCanBreak.note;
            app.preencherTimeSheet(null, modalIdGroup);
            $('#employeeModal').modal('show');
        }
    }
}
function actionFired(properties) {
    var results = items.get({
        filter: function (item) {
            return (item.open);
        }
    });
    results.forEach(function (item) {
        var hoje = moment().format("YYYYMMDD");
        var dia = item.start.format("YYYYMMDD");
        item.end =(VARS && VARS.test && VARS.test==='OK') ? moment(customTimeHour): getMoment();
        group = getGroupById(item.group);
        var objectCanBreak = canBreak(group.id);
        var cb = (objectCanBreak.canBreak || objectCanBreak.canLunch);
        var diffMinutes = item.end.diff(item.start, 'minutes');
        if (cb && group.className != 'toBreak90' && item.typeOfWork === 'Work' && diffMinutes >= 90 && diffMinutes < 120) {
            group.className = 'toBreak90';
            groups.update(group);
        } else if (cb && group.className != 'toBreak120' && item.typeOfWork === 'Work' && diffMinutes >= 120 && diffMinutes < 150) {
            group.className = 'toBreak120';
            groups.update(group);
        } else if (cb && group.className != 'toBreak150' && item.typeOfWork === 'Work' && diffMinutes >= 150) {
            group.className = 'toBreak150';
            groups.update(group);
        } else if (group.className != 'p' && item.typeOfWork === 'Work' && diffMinutes < 90) {
            group.className = 'p';
            groups.update(group);
        } else if (group.className === 'p' && item.typeOfWork != 'Work') {
            group.className = 'p';
            groups.update(group);
        }
        items.update(item);
    });
    showGroupStatus();
}
function getLastOpenItemBySubGroup(groupId) {
    var results = items.get({
        filter: function (item) {
            return (item.group == groupId && item.open);
        }
    });
    if (results.length > 0) {
        return results[0];
    } else {
        return null;
    }
}
function canBreak(groupId) {
    var result = {};
    result.canBreak = false;
    result.canLunch = false;
    result.note = "";
    var itemShift = getShiftTotalTime(groupId, 'minutes');
    var totalBreak = 0;
    var totalLunch = 0;
    var totalTimeBreak = 0;
    var totalTimeWork = 0;
    if (itemShift >= 240) {
        items.forEach(function (item) {
            if (item.group === groupId) {
                if (item.typeOfWork === 'Break' || item.typeOfWork === 'Lunch') {
                    if (item.typeOfWork === 'Break') {
                        totalBreak = totalBreak + 1;
                        if (typeof item.end != 'undefined' && item.end != null) {
                            totalTimeBreak = totalTimeBreak + (item.end.diff(item.start, 'minutes'));
                        }
                    } else if (item.typeOfWork === 'Lunch') {
                        totalLunch = totalLunch + 1;
                    }
                } else if (item.typeOfWork === 'Work') {
                    if (typeof item.end != 'undefined' && item.end != null) {
                        totalTimeWork = totalTimeWork + (item.end.diff(item.start, 'minutes'));
                    }
                }
            }
        });
        if (totalTimeWork > 0 && itemShift >= 240 && itemShift < 300) { // 10 minute break for a 4 hour shift
            result.canBreak = totalBreak < 1 && totalTimeBreak < 10;
            result.canLunch = false;
        } else if (totalTimeWork > 0 && itemShift >= 300 && itemShift < 360) { // 30 minute break for a 5 hour shift
            result.canBreak = false;
            result.canLunch = totalLunch < 1 && totalTimeBreak < 30;
        } else if (totalTimeWork > 0 && itemShift >= 360 && itemShift < 480) { // 30 minutes break and a 10 minute break for a 6 hour shift
            result.canBreak = totalBreak < 1 && totalTimeBreak < 10;
            result.canLunch = totalLunch < 1 && totalTimeBreak < 30;
        } else if (totalTimeWork > 0 && itemShift >= 480 && itemShift < 600) { // 30 minutes break and (2) 10 minute break for an 8 hour shift
            result.canBreak = totalBreak < 2 && totalTimeBreak < 20;
            result.canLunch = totalLunch < 1 && totalTimeBreak < 30;
        } else if (totalTimeWork > 0 && itemShift >= 600) { // 30 minutes break and (3) 10 minute break for an 8 hour shift
            result.canBreak = totalBreak < 3 && totalTimeBreak < 30;
            result.canLunch = totalLunch < 1 && totalTimeBreak < 30;
        }
        result.note = "S(" + itemShift + ") W(" + totalTimeWork + ") L(" + (result.canLunch ? "Y" : "N") + "," + totalLunch + ") B(" + (result.canBreak ? "Y" : "N") + "," + totalBreak + ":" + totalTimeBreak + ")";
    }
    if (VARS && VARS.test && VARS.test==='OK') {
        console.log(result);
    }
    return result;
}
function getShiftTotalTime(groupId, medida) {
    var itemShift = getScheduler(groupId);
    var allWorkItems = items.get({
        filter: function (item) {
            return (item.typeOfWork === 'Work');
        }
    });
    var firstAllWork = null;
    if (allWorkItems != null && allWorkItems.length > 0) {
        firstAllWork = allWorkItems[0];
        allWorkItems.forEach(function (work) {
            if (firstAllWork.orderDb > work.orderDb) {
                firstAllWork = work.orderDb;
            }
        });
    }
    var result = 0;
    if (firstAllWork != null) {
        result = itemShift.end.diff(firstAllWork.start, medida);
    } else {
        result = itemShift.end.diff(itemShift.start, medida);
    }
    return result;
}
function getScheduler(groupId) {
    var results = items.get({
        filter: function (item) {
            return (item.group == groupId && item.type == 'background');
        }
    });
    if (results.length > 0) {
        return results[0];
    } else {
        return null;
    }
}
function getGroupByName(groupName) {
    return groups.get({
        filter: function (item) {
            return (item.content == groupName);
        }
    });
}
function getGroupById(groupId) {
    return groups.get({
        filter: function (item) {
            return (item.id == groupId);
        }
    })[0];
}
function montarNome(idSubGroup, employeeName, checked) {
    var contentHtml = "";
    contentHtml = contentHtml + "<input class='form-check-input' onclick='checkme(this,\"" + idSubGroup + "\");' type='checkbox' " + (checked ? 'checked' : '') + ">";
    contentHtml = contentHtml + employeeName;
    return contentHtml;
}
function checkme(component, idSubGroup) {
    var subGrupo = groups.get(idSubGroup);
    var c = montarNome(subGrupo.id, subGrupo.employeeName, component.checked);
    groups.update({ id: idSubGroup, content: c, checked: !subGrupo.checked });
}
function removerAvisoVisual(idSubGroup) {
    var g = groups.get(idSubGroup);
    g.className = 'p';
    groups.update(g);
    persistItem(g, 'G');
}

function getLastOrderDb(idSubGroup) {
    var orderDb = 0;
    items.forEach(function (item) {
        if (item.group == idSubGroup) {
            if (orderDb < item.orderDb) {
                orderDb = item.orderDb;
            }
        }
    });
    return orderDb;
}

function closeLastItem(idSubGroup, hora, minuto) {
    if (typeof hora == 'undefined') hora = getHora(hora);
    if (typeof minuto == 'undefined') minuto = getMinuto(minuto);
    removerAvisoVisual(idSubGroup);
    var item = getLastOpenItemBySubGroup(idSubGroup);
    if (item) {
        item.end = getMoment().hours(hora).minutes(minuto).seconds(0).milliseconds(0);
        item.open = false;
        items.update(item);
        persistItem(item, 'I');
    }
}
function getHora(hora) {
    if (!hora) {
        if (!app || !app.form || !app.form.hora) {
            hora = getMoment().hours();
        } else {
            hora = app.form.hora;
        }
    }
    return hora;
}
function getMinuto(minuto) {
    if (!minuto) {
        if (!app || !app.form || !app.form.minuto) {
            minuto = getMoment().minutes();
        } else {
            minuto = app.form.minuto;
        }
    }
    return minuto;
}
function addItem(idSubGroup, hora, minuto, endHour, endMinute, typeOfWork, open, className) {
    if (typeof hora == 'undefined') hora = getHora(hora);
    if (typeof minuto == 'undefined') minuto = getMinuto(minuto);
    var orderDb = getLastOrderDb(idSubGroup);
    closeLastItem(idSubGroup, hora, minuto);
    var idItem = null;
    if (typeof endHour == 'undefined' && typeof endMinute == 'undefined') {
        idItem = items.add({
            group: idSubGroup,
            open: open,
            typeOfWork: typeOfWork,
            start: getMoment().hours(hora).minutes(minuto).seconds(0).milliseconds(0),
            persist: true,
            orderDb: orderDb + 1,
            className: className
        })[0];
    } else {
        idItem = items.add({
            group: idSubGroup,
            open: open,
            typeOfWork: typeOfWork,
            start: getMoment().hours(hora).minutes(minuto).seconds(0).milliseconds(0),
            end: getMoment().hours(endHour).minutes(endMinute).seconds(0).milliseconds(0),
            persist: true,
            orderDb: orderDb + 1,
            className: className
        })[0];
    }
    persistItem(items.get(idItem), 'I');
    $('#employeeModal').modal('hide');
}
function startme(idSubGroup, hora, minuto, endHour, endMinute) {
    addItem(idSubGroup, hora, minuto, endHour, endMinute, 'Work', true, 'green');
}
function breakme(idSubGroup, hora, minuto, endHour, endMinute) {
    var shift = getShiftTotalTime(idSubGroup, 'hours');
    var valid = true;
    if (shift < 4) {
        valid = confirm("This shift is less than 4 hour. Confirm break?");
    } else if (shift == 5) {
        valid = confirm("Shift 5 hours only lunch is available. Confirm break?");
    } else if (!canBreak(idSubGroup).canBreak) {
        valid = confirm("Confirm Break?");
    }
    if (valid) {
        addItem(idSubGroup, hora, minuto, endHour, endMinute, 'Break', true, 'red');
    }
}
function lunchme(idSubGroup, hora, minuto, endHour, endMinute) {
    var shift = getShiftTotalTime(idSubGroup, 'hours');
    var valid = true;
    if (shift < 5) {
        valid = confirm("This shift is less than 5 hour. Confirm Lunch?");
    } else if (!canBreak(idSubGroup).canLunch) {
        valid = confirm("Confirm Lunch?");
    }
    if (valid) {
        addItem(idSubGroup, hora, minuto, endHour, endMinute, 'Lunch', true, 'orange');
    }
}
function pushback(idSubGroup, diff) {
    var scheduler = getScheduler(idSubGroup);
    if (typeof diff === 'undefined') {
        diff = 1;
    }
    var hora = scheduler.start.hours();
    var minuto = scheduler.start.minutes();
    var endHour = scheduler.start.clone().add(diff, 'hours').hours();
    var endMinute = scheduler.start.minutes();
    addItem(idSubGroup, hora, minuto, endHour, endMinute, 'Push', false, 'aquamarine');
}
function homeme(idSubGroup, hora, minuto, endHour, endMinute) {
    var undefined;
    addItem(idSubGroup, hora, minuto, undefined, undefined, 'Home', false, undefined);
}
function adicionar(name, sector, id, horaInicial, minutoInicial, horaFinal, minutoFinal) {
    if (typeof name === 'undefined' || name == null || name.length <= 0) {
        return;
    }
    var idSubGroup = null;
    if (typeof id != 'undefined') {
        idSubGroup = groups.add({ id: id, employeeName: name, order: 0, checked: false, className: 'p' })[0];
    } else {
        idSubGroup = groups.add({ employeeName: name, order: 0, checked: false, className: 'p' })[0];
    }
    var c = montarNome(idSubGroup, name, false);
    groups.update({ id: idSubGroup, content: c });
    if (!horaInicial) horaInicial = 8;
    if (!minutoInicial) minutoInicial = 0;
    if (!horaFinal) horaFinal = horaInicial + 8;
    if (!minutoFinal) minutoFinal = 30;
    var id = items.add({
        group: idSubGroup,
        start: getMoment().hours(horaInicial).minutes(minutoInicial).seconds(0).milliseconds(0),
        end: getMoment().hours(horaFinal).minutes(minutoFinal).seconds(0).milliseconds(0),
        persist: true,
        orderDb: 0,
        type: 'background'
    })[0];
    var group = getGroupByName(sector);
    var idGroup;
    if (group.length == 0) {
        idGroup = groups.add({ content: sector, nestedGroups: [] })[0];
    } else {
        idGroup = group[0].id;
    }
    var g = groups.get(idGroup)
    g.nestedGroups.push(idSubGroup);
    groups.update(g);
    timeline.setGroups(groups);
    persistItem(groups.get(idSubGroup), 'G');
    persistItem(items.get(id), 'I');
    persistItem(groups.get(g.id), 'G');
    return idSubGroup;
}

function showGroupStatus() {
    var hoje = moment().format("YYYYMMDD");
    var diaTimeline = getMoment().format("YYYYMMDD");
    if (hoje === diaTimeline) {
        groups.forEach(function (group) {
            if (typeof group.employeeName === 'undefined' && typeof group.nestedGroups != 'undefined') {
                var qtdWorking = 0;
                var qtdBreaking = 0;
                var qtdLunching = 0;
                var itemStatus;
                groups.forEach(function (subGroup) {
                    if (subGroup.nestedInGroup === group.id) {
                        items.forEach(function (item) {
                            if (item.group === group.id && item.type != 'background') {
                                itemStatus = item;
                            } else if (item.group === subGroup.id) {
                                if (item.typeOfWork === 'Work' && item.open) {
                                    qtdWorking++;
                                } else if (item.typeOfWork === 'Lunch' && item.open) {
                                    qtdLunching++;
                                } else if (item.typeOfWork === 'Break' && item.open) {
                                    qtdBreaking++;
                                }
                            }
                        });
                    } else {
                        items.forEach(function (item) {
                            if (item.group === group.id) {
                                itemStatus = item;
                            }
                        });
                    }
                });
                var content = "" + qtdWorking + "F " + qtdBreaking + "B " + qtdLunching + "L " + (qtdWorking + qtdBreaking + qtdLunching) + "T";
                if (itemStatus != null && itemStatus.id != null) {
                    itemStatus.content = content;
                    items.update(itemStatus);
                } else if (group != null && group.id != null) {
                    items.add({
                        group: group.id,
                        start: getMoment().hours(9).minutes(0).seconds(0).milliseconds(0),
                        persist: false,
                        content: content
                    });
                }
            }
        });
    }
}
function fillSectors() {
    var group = getGroupByName("Wipedown");
    if (group.length == 0) {
        var id = groups.add({ id: 'Wipedown', order: 0, content: 'Wipedown', nestedGroups: [] })[0];
        persistItem(groups.get(id), 'G');
    }
    group = getGroupByName("Prep");
    if (group.length == 0) {
        var id = groups.add({ id: 'Prep', order: 1, content: 'Prep', nestedGroups: [] })[0];
        persistItem(groups.get(id), 'G');
    }
    group = getGroupByName("Detail");
    if (group.length == 0) {
        var id = groups.add({ id: 'Detail', order: 2, content: 'Detail', nestedGroups: [] })[0];
        persistItem(groups.get(id), 'G');
    }
    group = getGroupByName("Cash and Sale");
    if (group.length == 0) {
        var id = groups.add({ id: 'Cash and Sale', order: 3, content: 'Cash and Sale', nestedGroups: [] })[0];
        persistItem(groups.get(id), 'G');
    }
    timeline.setGroups(groups);
}

function persistItem(item, type) {
    const params = new URLSearchParams();
    var date = getMoment().format("YYYYMMDD");
    var jsonItem = JSON.stringify(item);
    params.append('date', date);
    params.append('item', jsonItem);
    axios.post('/ts', params);
    var message = {};
    message.action = 'PERSIST';
    message.type = type;
    message.item = item;
    message.from = IDENTIFIER;
    send(JSON.stringify(message));
}

function removeItem(item, type) {
    const params = {};
    params.date = getMoment().format("YYYYMMDD");
    params.item = JSON.stringify(item);
    axios.delete('/ts?date=' + params.date + '&itemId=' + item.id, params);
    var message = {};
    message.action = 'REMOVE';
    message.type = type;
    message.item = item;
    message.from = IDENTIFIER;
    send(JSON.stringify(message));
}

function uuidv4() {
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
        (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
}

function updateDateItem(item) {
    if (typeof item.start != 'undefined') {
        item.start = moment(item.start);
    }
    if (typeof item.end != 'undefined') {
        item.end = moment(item.end);
    }
}

var socket;
if (window.WebSocket) {
    var url = "wss://" + location.hostname + "/ws";
    socket = new WebSocket(url);
    socket.onmessage = function (event) {
        var action = JSON.parse(event.data);
        if (action.from === IDENTIFIER) {
            return;
        }
        if (action.action === 'PERSIST') {
            if (action.type === 'G') {
                var g = groups.get(action.item.id);
                updateDateItem(action.item);
                if (g == null) {
                    groups.add(action.item);
                } else {
                    groups.update(action.item);
                }
            } else if (action.type === 'I') {
                var i = items.get(action.item.id);
                updateDateItem(action.item);
                if (i == null) {
                    items.add(action.item);
                } else {
                    items.update(action.item);
                }
            }
        } else if (action.action === 'REMOVE') {
            if (action.type === 'G') {
                var group = groups.get(action.item.id);
                if (group != null) {
                    groups.remove(group);
                }
            } else if (action.type === 'I') {
                var i = items.get(action.item.id);
                if (i != null) {
                    items.remove(i);
                }
            }
        } else if (action.action === 'IMPORT') {
            load();
        }
        timeline.fit();
    };
    socket.onopen = function (event) {
    };
    socket.onclose = function (event) {
    };
} else {
    console.log("Your browser does not support Websockets. (Use Chrome)");
}
function send(message) {
    if (!window.WebSocket) {
        return;
    }
    if (socket.readyState == WebSocket.OPEN) {
        socket.send(message);
    } else {
        console.log("The socket is not open.");
    }
}

function load() {
    timeline.off('currentTimeTick');
    axios.get('/ts', {
        params: {
            date: getMoment().format("YYYYMMDD")
        }
    }).then(function (response) {
        response.data.groups.forEach(function (item) {
            if (typeof item.start != 'undefined') {
                item.start = moment(item.start);
            }
            if (typeof item.end != 'undefined') {
                item.end = moment(item.end);
            }
        });
        groups.clear();
        groups.add(response.data.groups);
        response.data.items.forEach(function (item) {
            if (typeof item.start != 'undefined') {
                item.start = moment(item.start);
            }
            if (typeof item.end != 'undefined') {
                item.end = moment(item.end);
            }
        });
        items.clear();
        items.add(response.data.items);
        fillSectors();
        showGroupStatus();
        timeline.on('currentTimeTick', actionFired);
    }).catch(function (error) {
        groups.clear();
        items.clear();
        fillSectors();
        timeline.on('currentTimeTick', actionFired);
    });
    timeline.fit();
}

load();