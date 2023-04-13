const BASE_URL = location.protocol + '//' + location.hostname + (location.port.toString().trim().length !== 0 ? ':' + location.port : '');

const changeView = () => {
    const elements = document.getElementsByName("view-option");
    if (!elements) {
        return;
    }
    let view = "";
    for (let i = 0; i < elements.length; i++) {
        const element = elements[i];
        if (element.checked) {
            view = element.value;
            break;
        }
    }
    
    if (view) {
        window.location.href = `/?view=${view}`;
    } else {
        window.location.href = "/";
    }
}

const addToSession = (view, url, title, buttonId=null) => {
    if (!title) {
        title = "[No Title]";
    }
    try {
        axios.post(
            `/api/session?view=${view}`, 
            `${url}\n${title}`,
            {headers: {"Content-Type": "text/plain"}}
        ).then((res) => {
            if (view === "favorites" && buttonId) {
                document.getElementById(buttonId).disabled = true;
            }
        }).catch((err) => {
            alert(err.message);
        });
    } catch (err) {
        alert(err.message);
    }
}

const removeFavorite = async (elementId, url) => {
    try {
        await axios.delete(`/api/session?view=favorites&url=${url}`);
        const element = document.getElementById(elementId);
        if (element) {
            element.remove();
        }
    } catch (err) {
        alert(err.message);
    }
}

const renderPage = () => {
    // Get query params
    const urlSearchParams = new URLSearchParams(window.location.search);
    const params = Object.fromEntries(urlSearchParams.entries());
    const view = params.view ? params.view : "search";

    // Render appropriate view
    if (view === "search") {
        const q = params.q ? encodeURIComponent(params.q.trim()) : "";
        getAndDisplaySearchResults(q);
    } else if (view === "history") {
        getAndDisplayHistory();
    } else if (view === "visited") {
        getAndDisplayVisited();
    } else if (view === "favorites") {
        getAndDisplayFavorites();
    }

    // Check the appropriate view in the options modal
    const element = document.getElementById(`${view}-view`);
    if (element) {
        element.checked = true;
    }
}

const getAndDisplaySearchResults = async (q) => {
    if (!q) {
        return;
    }
    try {
        document.getElementById("search").setAttribute("value", q);

        const startTime = performance.now();
        const res = await axios.get(`/api/search?q=${q}`);
        const endTime = performance.now();
        // Add to history
        addToSession("history", `${BASE_URL}?q=${q}`, q);
        const container = document.getElementById("search-list-container");
        if (Array.isArray(res.data) && res.data.length > 0) {
            document.getElementById("execution-stats").innerHTML = `${res.data.length} ${res.data.length === 1 ? "result" : "results"} in about ${((endTime - startTime) / 1000).toFixed(2)} seconds`;
            const ul = document.createElement("ul");
            ul.classList.add("list-group");
            ul.classList.add("list-group-flush");
            ul.style.marginTop = "15px";
            res.data.forEach((d) => {
                const date = new Date(parseInt(d.timestamp));
                const li = document.createElement("li");
                li.classList.add("list-group-item");
                li.style.border = "none";
                li.style.marginBottom = "12px";
                li.innerHTML = `
                    <div class="row">
                        <div class="col">
                            <div style="font-size: 10px">${d.where}</div>
                            <a href="${d.where}" target="_blank" style="text-decoration: none" onclick="addToSession('visited', '${d.where}', '${d.title}')">${d.title ? d.title : "[No Title]"}</a>
                            <div style="font-size: 12px;">Frequency: ${d.count} | Score: ${((d.score)*100).toFixed(2)}% | Processed on ${date.toLocaleDateString() + " at " + date.toLocaleTimeString()}</div>
                        </div>
                    </div>
                `;
                ul.appendChild(li);
            });
            container.appendChild(ul);
        } else {
            document.getElementById("execution-stats").innerHTML = `0 results in about ${((endTime - startTime) / 1000).toFixed(2)} seconds`;
            container.classList.add("center-align");
            container.innerHTML = `
                <br>
                <br>
                <br>
                <div>No results found for <strong>'${q}'</strong></div>
            `;
        }
    } catch (err) {
        alert(err.message);
    }
}

const getAndDisplayHistory = async () => {
    try {
        const res = await axios.get(`/api/session?view=history`);
        const data = res.data ? res.data : [];
        data.sort((a, b) => {
            return b.timestamp - a.timestamp;
        });
        displayUrlAndTimestamps(data, "📜 Search History", "No history...", "Searched on", true);
    } catch (err) {
        alert(err.message);
    }
}

const getAndDisplayVisited = async () => {
    try {
        const res = await axios.get(`/api/session?view=visited`);
        const data = res.data ? res.data : [];
        data.sort((a, b) => {
            return b.timestamp - a.timestamp;
        });
        displayUrlAndTimestamps(data, "🖱️ Pages Visited", "No pages visited...", "Visited on", true);
    } catch (err) {
        alert(err.message);
    }
}

const getAndDisplayFavorites = async () => {
    try {
        const res = await axios.get(`/api/session?view=favorites`);
        const data = res.data ? res.data : [];
        data.sort((a, b) => {
            return b.timestamp - a.timestamp;
        });
        displayUrlAndTimestamps(data, "⭐ Favorites", "No favorites...", "Favorited on");
    } catch (err) {
        alert(err.message);
    }
}

const displayUrlAndTimestamps = (data, titleDisplay, noDataDisplay, timestampPrefix, withFavorite=false) => {
    const container = document.getElementById("search-list-container");
        
    const header = document.createElement("h4");
    header.innerHTML = `${titleDisplay}`;
    container.appendChild(header);

    if (data.length > 0) {
        const ul = document.createElement("ul");
        ul.classList.add("list-group");
        ul.classList.add("list-group-flush");
        ul.style.marginTop = "15px";
        data.forEach((d, i) => {
            const date = new Date(d.timestamp);
            const li = document.createElement("li");
            li.classList.add("list-group-item");
            li.style.border = "none";
            li.style.marginBottom = "12px";
            const liId = `urlTimestamp${i}`;
            li.id = liId;
            if (withFavorite) {
                const favoriteButtonId = `favoriteButton${i}`;
                li.innerHTML = `
                    <div class="row">
                        <div class="col">
                            <div style="font-size: 10px">${d.url}</div>
                            <a href="${d.url}" style="text-decoration: none">${d.title}</a>
                            <div style="font-size: 12px">${timestampPrefix} ${date.toLocaleDateString() + " at " + date.toLocaleTimeString()}</div>
                        </div>
                        <div class="col" style="text-align: right">
                            ${d.isFavorite 
                                ? `<button id="${favoriteButtonId}" type="button" class="btn btn-light btn-sm" disabled>Favorite ⭐</button>`
                                :`<button id="${favoriteButtonId}" type="button" class="btn btn-light btn-sm" onclick="addToSession('favorites', '${d.url}', '${d.title}', '${favoriteButtonId}')">Favorite ⭐</button>` 
                            }
                        </div>
                    </div>
                 `;   
            } else {
                li.innerHTML = `
                    <div class="row">
                        <div class="col">
                            <div style="font-size: 10px">${d.url}</div>
                                <a href="${d.url}" style="text-decoration: none">${d.title}</a>
                            <div style="font-size: 12px">${timestampPrefix} ${date.toLocaleDateString() + " at " + date.toLocaleTimeString()}</div>
                        </div>
                        <div class="col" style="text-align: right">
                            <button type="button" class="btn btn-light btn-sm" onclick="removeFavorite('${liId}', '${d.url}')">Remove 🗑️</button>
                        </div>
                    </div>
                `;   
            }
            ul.appendChild(li);
        });
        container.appendChild(ul);
    } else {
        container.innerHTML += `
            <br>
            <div style="text-align: center">${noDataDisplay}</div>
        `;
    }
}