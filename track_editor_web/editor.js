// 赛道编辑器 JavaScript

let map;
let currentMode = null; // 'start', 'end', 'checkpoint'
let drawingPolygon = null;
let drawnPolygons = {
    start: null,
    end: null,
    checkpoints: []
};
let currentCheckpointIndex = -1;
let rules = [];

// 初始化地图
function initMap() {
    // 使用北京作为默认中心点
    map = new AMap.Map('map', {
        zoom: 15,
        center: [116.5120, 40.0080],
        viewMode: '2D'
    });

    // 添加点击事件
    map.on('click', function(e) {
        if (!currentMode) return;
        
        const lnglat = e.lnglat;
        addPointToPolygon(lnglat);
    });

    console.log('地图初始化完成');
}

// 搜索地点
function searchLocation() {
    const input = document.getElementById('search-input').value.trim();
    if (!input) return;

    // 检查是否是经纬度格式 (lat,lng)
    const latLngMatch = input.match(/^(-?\d+\.\d+),\s*(-?\d+\.\d+)$/);
    if (latLngMatch) {
        const lat = parseFloat(latLngMatch[1]);
        const lng = parseFloat(latLngMatch[2]);
        map.setCenter([lng, lat]);
        map.setZoom(16);
        new AMap.Marker({ position: [lng, lat], map: map });
        return;
    }

    // 使用高德地图 Geocoder 插件进行地址搜索
    AMap.plugin('AMap.Geocoder', function() {
        const geocoder = new AMap.Geocoder({
            city: "全国",
        });

        geocoder.getLocation(input, function(status, result) {
            if (status === 'complete' && result.geocodes.length) {
                const lnglat = result.geocodes[0].location;
                map.setCenter(lnglat);
                map.setZoom(16);
                new AMap.Marker({ position: lnglat, map: map });
            } else {
                alert('未找到该地点，请尝试更详细的地址或检查输入。');
            }
        });
    });
}

// 设置绘制模式
function setMode(mode) {
    currentMode = mode;
    drawingPolygon = [];
    
    // 启用完成和取消按钮
    document.getElementById('finish-btn').disabled = false;
    document.getElementById('cancel-btn').disabled = false;
    
    const modeInfo = document.getElementById('mode-info');
    switch(mode) {
        case 'start':
            modeInfo.textContent = '正在绘制起点围栏：点击地图添加顶点，完成后点击"完成绘制"按钮';
            break;
        case 'end':
            modeInfo.textContent = '正在绘制终点围栏：点击地图添加顶点，完成后点击"完成绘制"按钮';
            break;
        case 'checkpoint':
            modeInfo.textContent = '正在绘制检查点：点击地图添加顶点，完成后点击"完成绘制"按钮';
            break;
    }
    
    console.log('切换到模式:', mode);
}

// 添加点到多边形
function addPointToPolygon(lnglat) {
    if (!drawingPolygon) return;
    
    drawingPolygon.push({
        latitude: lnglat.getLat(),
        longitude: lnglat.getLng()
    });
    
    // 根据当前模式选择不同颜色的标记
    let markerColor = '#FF3D00'; // 默认红色（起点）
    let markerSize = 10;
    
    if (currentMode === 'end') {
        markerColor = '#4CAF50'; // 绿色（终点）
    } else if (currentMode === 'checkpoint') {
        markerColor = '#2196F3'; // 蓝色（检查点）
    }
    
    // 在地图上显示临时标记
    const marker = new AMap.Marker({
        position: lnglat,
        icon: new AMap.Icon({
            size: new AMap.Size(markerSize, markerSize),
            image: `data:image/svg+xml;base64,${btoa(`<svg width="${markerSize}" height="${markerSize}" xmlns="http://www.w3.org/2000/svg"><circle cx="${markerSize/2}" cy="${markerSize/2}" r="${markerSize/2}" fill="${markerColor}"/></svg>`)}`,
            imageSize: new AMap.Size(markerSize, markerSize)
        })
    });
    map.add(marker);
    
    console.log('添加点:', lnglat.getLat(), lnglat.getLng());
}

// 完成多边形绘制
function finishPolygon() {
    if (!drawingPolygon || drawingPolygon.length < 3) {
        alert('至少需要3个点才能形成多边形');
        return;
    }
    
    // 根据模式选择不同颜色
    let strokeColor = '#FF3D00'; // 默认红色（起点）
    let fillColor = '#FF3D00';
    
    if (currentMode === 'end') {
        strokeColor = '#4CAF50'; // 绿色（终点）
        fillColor = '#4CAF50';
    } else if (currentMode === 'checkpoint') {
        strokeColor = '#2196F3'; // 蓝色（检查点）
        fillColor = '#2196F3';
    }
    
    // 在地图上绘制多边形
    const polygon = new AMap.Polygon({
        path: drawingPolygon.map(p => [p.longitude, p.latitude]),
        strokeColor: strokeColor,
        strokeWeight: 2,
        fillColor: fillColor,
        fillOpacity: 0.3
    });
    
    map.add(polygon);
    
    // 保存绘制的多边形
    if (currentMode === 'start') {
        if (drawnPolygons.start) {
            map.remove(drawnPolygons.start.polygon);
        }
        drawnPolygons.start = {
            polygon: polygon,
            points: drawingPolygon
        };
        alert('起点围栏绘制完成！');
    } else if (currentMode === 'end') {
        if (drawnPolygons.end) {
            map.remove(drawnPolygons.end.polygon);
        }
        drawnPolygons.end = {
            polygon: polygon,
            points: drawingPolygon
        };
        alert('终点围栏绘制完成！');
    } else if (currentMode === 'checkpoint') {
        const checkpointName = prompt('输入检查点名称:', `检查点${drawnPolygons.checkpoints.length + 1}`);
        if (checkpointName) {
            const checkpoint = {
                name: checkpointName,
                sequence: drawnPolygons.checkpoints.length + 1,
                polygon: polygon,
                points: drawingPolygon,
                description: '',
                rules: []
            };
            drawnPolygons.checkpoints.push(checkpoint);
            updateCheckpointsList();
            alert(`检查点 "${checkpointName}" 绘制完成！`);
        }
    }
    
    // 重置绘制状态
    drawingPolygon = null;
    currentMode = null;
    document.getElementById('mode-info').textContent = '请先选择绘制模式，然后点击地图添加顶点';
    
    // 禁用完成和取消按钮
    document.getElementById('finish-btn').disabled = true;
    document.getElementById('cancel-btn').disabled = true;
}

// 更新检查点列表
function updateCheckpointsList() {
    const listDiv = document.getElementById('checkpoints-list');
    const section = document.getElementById('checkpoints-section');
    const select = document.getElementById('selected-checkpoint');
    
    let htmlContent = '';
    
    // 添加起点围栏信息
    if (drawnPolygons.start) {
        htmlContent += `
            <div class="start-fence-item">
                <h3>起点围栏</h3>
                <p>顶点数: ${drawnPolygons.start.points.length}</p>
                <button onclick="clearStartFence()" class="btn-danger" style="padding: 5px 10px; font-size: 12px;">删除</button>
            </div>
        `;
    }
    
    // 添加终点围栏信息
    if (drawnPolygons.end) {
        htmlContent += `
            <div class="end-fence-item">
                <h3>终点围栏</h3>
                <p>顶点数: ${drawnPolygons.end.points.length}</p>
                <button onclick="clearEndFence()" class="btn-danger" style="padding: 5px 10px; font-size: 12px;">删除</button>
            </div>
        `;
    }
    
    // 添加检查点信息
    if (drawnPolygons.checkpoints.length > 0) {
        section.style.display = 'block';
        document.getElementById('rules-section').style.display = 'block';
        
        htmlContent += drawnPolygons.checkpoints.map((cp, index) => `
            <div class="checkpoint-item" style="border-left-color: #2196F3;">
                <h3>${cp.name}</h3>
                <p>顶点数: ${cp.points.length}</p>
                <p>规则数: ${cp.rules.length}</p>
                <button onclick="deleteCheckpoint(${index})" class="btn-danger" style="padding: 5px 10px; font-size: 12px;">删除</button>
            </div>
        `).join('');
    } else {
        section.style.display = 'none';
        document.getElementById('rules-section').style.display = 'none';
    }
    
    listDiv.innerHTML = htmlContent;
    
    // 更新下拉选择框（仅包含检查点）
    select.innerHTML = drawnPolygons.checkpoints.map((cp, index) => 
        `<option value="${index}">${cp.name}</option>`
    ).join('');
    
    // 监听选择变化
    select.onchange = function() {
        currentCheckpointIndex = parseInt(this.value);
        displayRulesForCheckpoint(currentCheckpointIndex);
    };
    
    if (drawnPolygons.checkpoints.length > 0 && currentCheckpointIndex === -1) {
        currentCheckpointIndex = 0;
        displayRulesForCheckpoint(0);
    }
}

// 显示检查点的规则
function displayRulesForCheckpoint(index) {
    const cp = drawnPolygons.checkpoints[index];
    const rulesDiv = document.getElementById('current-rules-display');
    if (!rulesDiv) return;
    
    if (cp.rules.length === 0) {
        rulesDiv.innerHTML = '<p style="color: #999; font-size: 13px;">暂无规则</p>';
        return;
    }

    rulesDiv.innerHTML = cp.rules.map((rule, i) => `
        <div class="rule-item" style="margin-bottom: 8px; border-left: 3px solid #2196F3; padding-left: 8px;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <strong>${getRuleTypeName(rule.ruleType)}</strong>
                <button onclick="deleteRule(${index}, ${i})" class="btn-danger" style="padding: 2px 8px; font-size: 11px;">删除</button>
            </div>
            <p style="font-size: 12px; color: #666; margin-top: 4px;">${rule.description}</p>
            <pre style="font-size: 11px; background: #eee; padding: 4px; margin-top: 4px; border-radius: 2px;">${JSON.stringify(rule.parameters, null, 2)}</pre>
        </div>
    `).join('');
}

// 获取规则类型名称
function getRuleTypeName(type) {
    const names = {
        'min_points': '最少点数',
        'speed_limit': '速度限制',
        'time_limit': '时间限制',
        'mandatory_stop': '强制停留'
    };
    return names[type] || type;
}

// 删除检查点
function deleteCheckpoint(index) {
    if (confirm('确定要删除这个检查点吗？')) {
        map.remove(drawnPolygons.checkpoints[index].polygon);
        drawnPolygons.checkpoints.splice(index, 1);
        
        // 重新编号
        drawnPolygons.checkpoints.forEach((cp, i) => {
            cp.sequence = i + 1;
        });
        
        updateCheckpointsList();
    }
}

// 清除起点围栏
function clearStartFence() {
    if (confirm('确定要删除起点围栏吗？')) {
        if (drawnPolygons.start) {
            map.remove(drawnPolygons.start.polygon);
            drawnPolygons.start = null;
            updateCheckpointsList();
        }
    }
}

// 清除终点围栏
function clearEndFence() {
    if (confirm('确定要删除终点围栏吗？')) {
        if (drawnPolygons.end) {
            map.remove(drawnPolygons.end.polygon);
            drawnPolygons.end = null;
            updateCheckpointsList();
        }
    }
}

// 清除当前绘制
function clearCurrent() {
    if (currentMode === 'start' && drawnPolygons.start) {
        if (confirm('确定要删除起点围栏吗？')) {
            map.remove(drawnPolygons.start.polygon);
            drawnPolygons.start = null;
            updateCheckpointsList();
        }
    } else if (currentMode === 'end' && drawnPolygons.end) {
        if (confirm('确定要删除终点围栏吗？')) {
            map.remove(drawnPolygons.end.polygon);
            drawnPolygons.end = null;
            updateCheckpointsList();
        }
    }
    drawingPolygon = [];
}

// 取消当前绘制
function cancelDrawing() {
    drawingPolygon = [];
    currentMode = null;
    document.getElementById('mode-info').textContent = '请先选择绘制模式，然后点击地图添加顶点';
    
    // 禁用完成和取消按钮
    document.getElementById('finish-btn').disabled = true;
    document.getElementById('cancel-btn').disabled = true;
    
    console.log('取消当前绘制');
}

// 添加规则
function addRule() {
    if (currentCheckpointIndex === -1) {
        alert('请先选择一个检查点');
        return;
    }
    
    const ruleType = document.getElementById('rule-type').value;
    const description = document.getElementById('rule-description').value;
    
    let parameters = {};
    
    // 根据规则类型收集参数
    if (ruleType === 'min_points') {
        const minPoints = document.getElementById('param-min-points');
        const speedThreshold = document.getElementById('param-speed-threshold');
        if (minPoints && speedThreshold) {
            parameters = {
                min_points: parseInt(minPoints.value),
                speed_threshold: parseFloat(speedThreshold.value)
            };
        }
    } else if (ruleType === 'speed_limit') {
        const maxSpeed = document.getElementById('param-max-speed');
        const minSpeed = document.getElementById('param-min-speed');
        if (maxSpeed && minSpeed) {
            parameters = {
                max_speed: parseFloat(maxSpeed.value),
                min_speed: parseFloat(minSpeed.value)
            };
        }
    } else if (ruleType === 'time_limit') {
        const maxTime = document.getElementById('param-max-time');
        if (maxTime) {
            parameters = {
                max_time: parseFloat(maxTime.value)
            };
        }
    } else if (ruleType === 'mandatory_stop') {
        const stopDuration = document.getElementById('param-stop-duration');
        const stopSpeed = document.getElementById('param-stop-speed');
        if (stopDuration && stopSpeed) {
            parameters = {
                stop_duration: parseFloat(stopDuration.value),
                stop_speed: parseFloat(stopSpeed.value)
            };
        }
    }
    
    const rule = {
        ruleType: ruleType,
        parameters: parameters,
        description: description || '未命名规则'
    };
    
    drawnPolygons.checkpoints[currentCheckpointIndex].rules.push(rule);
    updateCheckpointsList();
    displayRulesForCheckpoint(currentCheckpointIndex);
    
    // 清空描述
    document.getElementById('rule-description').value = '';
    
    alert('规则添加成功！');
}

// 删除规则
function deleteRule(checkpointIndex, ruleIndex) {
    drawnPolygons.checkpoints[checkpointIndex].rules.splice(ruleIndex, 1);
    updateCheckpointsList();
    displayRulesForCheckpoint(checkpointIndex);
}

// 监听规则类型变化，显示不同的参数输入
document.getElementById('rule-type').onchange = function() {
    const paramsDiv = document.getElementById('rule-params');
    const ruleType = this.value;
    
    let html = '';
    if (ruleType === 'min_points') {
        html = `
            <label for="param-min-points">最少有效点数</label>
            <input type="number" id="param-min-points" placeholder="例如: 2" value="2">
            <label for="param-speed-threshold">最低通过速度 (km/h)</label>
            <input type="number" id="param-speed-threshold" placeholder="低于此速度不计点" value="5" step="0.1">
        `;
    } else if (ruleType === 'speed_limit') {
        html = `
            <label for="param-max-speed">最高限速 (km/h)</label>
            <input type="number" id="param-max-speed" placeholder="例如: 80" value="80" step="0.1">
            <label for="param-min-speed">最低限速 (km/h)</label>
            <input type="number" id="param-min-speed" placeholder="例如: 30" value="30" step="0.1">
        `;
    } else if (ruleType === 'time_limit') {
        html = `
            <label for="param-max-time">最大允许用时 (秒)</label>
            <input type="number" id="param-max-time" placeholder="从起点到此处的最大时间" value="120" step="0.1">
        `;
    } else if (ruleType === 'mandatory_stop') {
        html = `
            <label for="param-stop-duration">最短停留时间 (秒)</label>
            <input type="number" id="param-stop-duration" placeholder="必须停车至少多少秒" value="3" step="0.1">
            <label for="param-stop-speed">判定停车速度 (km/h)</label>
            <input type="number" id="param-stop-speed" placeholder="低于此速度视为停车" value="2" step="0.1">
        `;
    }
    
    paramsDiv.innerHTML = html;
};

// 导出赛道为JSON
function exportTrack() {
    if (!drawnPolygons.start || !drawnPolygons.end) {
        alert('请至少绘制起点和终点围栏！');
        return;
    }
    
    if (drawnPolygons.checkpoints.length === 0) {
        if (!confirm('没有检查点，确定要继续吗？')) {
            return;
        }
    }
    
    const trackData = {
        name: document.getElementById('track-name').value || '未命名赛道',
        description: document.getElementById('track-description').value || '',
        length: parseFloat(document.getElementById('track-length').value) || 0,
        startPolygon: drawnPolygons.start.points,
        endPolygon: drawnPolygons.end.points,
        thumbnailUrl: null,
        publishedAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        checkPoints: drawnPolygons.checkpoints.map(cp => ({
            name: cp.name,
            sequence: cp.sequence,
            polygon: cp.points,
            description: cp.description,
            rules: cp.rules
        }))
    };
    
    const jsonString = JSON.stringify(trackData, null, 2);
    document.getElementById('json-output').value = jsonString;
    
    alert('JSON生成成功！可以复制或下载');
}

// 下载JSON文件
function downloadJSON() {
    const jsonString = document.getElementById('json-output').value;
    if (!jsonString) {
        alert('请先生成JSON');
        return;
    }
    
    const blob = new Blob([jsonString], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${document.getElementById('track-name').value || 'track'}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

// 页面加载完成后初始化地图
window.onload = function() {
    // 注意：需要替换为你自己的高德地图API Key
    if (typeof AMap === 'undefined') {
        console.error('高德地图API未加载，请检查API Key');
        alert('地图加载失败，请检查网络连接和API Key配置');
    } else {
        initMap();
    }
};
