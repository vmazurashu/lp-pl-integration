zk.$package("ganttz");

ganttz.DependencyComponentBase = zk.$extends(
    zul.Widget,
    {
        $define : {
            idTaskOrig : null,
            idTaskEnd : null,
            dependencyType : null
        },

        bind_ : function(){
            this.$supers('bind_', arguments);
            this.setupArrow_();
        },

        setCSSClass: function(newClass) {
            this.$n().setAttribute("class", newClass);
        },

        draw : function() {
            throw "draw method must be overwriten by extending classes"
        },

        drawArrow_ : function(coordOrig, coordDest) {
            switch(this.getDependencyType()) {
                case this.$class.START_START:
                    this._drawArrowStartStart(coordOrig, coordDest);
                    break;

                case this.$class.END_END:
                    this._drawArrowEndEnd(coordOrig, coordDest);
                    break;

                case this.$class.END_START:
                default:
                    this._drawArrowEndStart(coordOrig, coordDest);
            }
        },

        _drawArrowStartStart : function(coordOrig, coordDest) {
            var yorig = coordOrig.top - ganttz.TaskComponent.CORNER_WIDTH/2 + this.$class.HALF_DEPENDENCY_PADDING;
            var xorig = coordOrig.left - this.$class.HALF_DEPENDENCY_PADDING;
            var xend = coordDest.left + this.$class.HALF_DEPENDENCY_PADDING;
            var yend = coordDest.top - this.$class.HALF_DEPENDENCY_PADDING;

            if (yend < yorig) {
                yorig = coordOrig.top + this.$class.DEPENDENCY_PADDING;
            }


            var width1 = ganttz.TaskComponent.CORNER_WIDTH;
            var width2 = Math.abs(xend - xorig) + ganttz.TaskComponent.CORNER_WIDTH;
            var height = Math.abs(yend - yorig);

            if (xorig > xend) {
                width1 = width2;
                width2 = ganttz.TaskComponent.CORNER_WIDTH;
            }

            // First segment
            var depstart = this._findImageElement('start');
            depstart.css({
                top : yorig,
                left : (xorig - width1),
                width : width1 ,
                display : 'inline'
            });

            // Second segment
            var depmid = this._findImageElement('mid');
            var depmidcss = { left : depstart.css('left'), height : height };

            if (yend > yorig) {
                depmidcss.top = yorig;
            } else {
                depmidcss.top = yend;
            }

            depmid.css(depmidcss);

            // Third segment
            var depend = this._findImageElement('end');
            depend.css({
                top : yend,
                left : depstart.css('left'),
                width : width2 - ganttz.TaskComponent.HALF_HEIGHT
            });

            var deparrow = this._findImageElement('arrow');
            deparrow.removeClass("point-north point-south point-west");
            deparrow.addClass("point-east");
            deparrow.css({top : (yend - ganttz.TaskComponent.HALF_HEIGHT),left : xend - 15});
        },

        _drawArrowEndEnd : function(coordOrig, coordDest) {
            var xorig = coordOrig.left - this.$class.DEPENDENCY_PADDING;
            var yorig = coordOrig.top - ganttz.TaskComponent.CORNER_WIDTH/2 + this.$class.HALF_DEPENDENCY_PADDING;
            var xend = coordDest.left + this.$class.HALF_DEPENDENCY_PADDING;
            var yend = coordDest.top - this.$class.DEPENDENCY_PADDING;

            var width1 = Math.abs(xend - xorig) + ganttz.TaskComponent.CORNER_WIDTH;
            var width2 = ganttz.TaskComponent.CORNER_WIDTH;
            var height = Math.abs(yend - yorig);

            if (xorig > xend) {
                width2 = width1;
                width1 = ganttz.TaskComponent.CORNER_WIDTH;
            }

            // First segment
            var depstart = this._findImageElement('start');
            var depstartcss = { left : xorig, width : width1, display : 'inline' };

            if (yend > yorig)
                depstartcss.top = yorig ;
            else
                depstartcss.top = yorig + ganttz.TaskComponent.HEIGHT;

            depstart.css(depstartcss);

            // Second segment
            var depmid = this._findImageElement('mid');
            var depmidcss = { left : (xorig + width1), height : height };

            if (yend > yorig) {
                depmidcss.top = yorig;
            } else {
                depmidcss.top = yend;
                depmidcss.height = height + 10;
            }
            depmid.css(depmidcss);

            // Third segment
            var depend = this._findImageElement('end');
            depend.css({
                left : (xorig + width1 - width2),
                top:yend,
                width:width2
            });


            var deparrow = this._findImageElement('arrow');
            deparrow.removeClass("point-north point-south point-east");
            deparrow.addClass("point-west");
            deparrow.css({top : yend - 5, left : xend - 8});
        },

        _drawArrowEndStart : function(coordOrig, coordDest) {
            var xorig = coordOrig.left - this.$class.DEPENDENCY_PADDING;
            var yorig = coordOrig.top - this.$class.HALF_DEPENDENCY_PADDING;
            var xend = coordDest.left - this.$class.DEPENDENCY_PADDING;
            var yend = coordDest.top - this.$class.HALF_DEPENDENCY_PADDING;

            var width = (xend - xorig);
            var xmid = xorig + width;

            // First segment not used
            var depstart = this._findImageElement('start');
            depstart.hide();

            // Second segment not used
            var depmid = this._findImageElement('mid');
            var depmidcss;

            if (yend > yorig) {
                depmidcss = { top : yorig, height : yend - yorig };
            }
            else {
                depmidcss = { top : yend, height : (yorig - yend) };
            }


            depmidcss.left = xorig;
            depmid.css(depmidcss);

            var depend = this._findImageElement('end');
            var dependcss = { top : yend, left : xorig, width : width };

            if (width < 0) {
                dependcss.left = xend;
                dependcss.width = Math.abs(width);
            }
            depend.css(dependcss);

            var deparrow = this._findImageElement('arrow');
            var deparrowcss;

            if ( width == 0 ) {
                deparrow.removeClass("point-north point-west point-east");
                deparrow.addClass("point-south");
                deparrowcss = { top : (yend - 10) , left : (xend - 5) };

                if ( yorig > yend ) {
                    deparrow.removeClass("point-west point-south point-east");
                    deparrow.addClass("point-north");
                    deparrowcss = {top : yend};
                }
            } else {
                deparrowcss = {top : (yend -5), left : (xend - 10)};
                deparrow.removeClass("point-north point-south point-west");
                deparrow.addClass("point-east");
                if (width < 0) {
                    deparrow.removeClass("point-north point-south point-east");
                    deparrow.addClass("point-west");
                    deparrowcss = {top : (yend - 5), left : xend}
                }
            }
            deparrow.css(deparrowcss);
        },

        findPos_ : function(element) {
            var pos1 = jq('#listdependencies').offset();
            var pos2 = element.offset();
            return { left : (pos2.left - pos1.left), top : (pos2.top - pos1.top) };
        },

        _findImageElement : function(name) {
            return jq('.' + name + '', this.$n());
        },

        setupArrow_ : function() {
            var image_data = [ "start" , "mid" , "end", "arrow" ] ;
            var imgDiv;
            var insertPoint = jq(this.$n());

            for ( var i = 0; i < image_data.length; i++) {
                imgDiv = jq(document.createElement('div'));
                imgDiv.attr('class', image_data[i]);
                insertPoint.append(imgDiv);
            }
        }
    },

    {
        END_END : "END_END",
        END_START : "END_START",
        START_START : "START_START",
        HALF_DEPENDENCY_PADDING : 2,
        DEPENDENCY_PADDING : 4,
        DRAGABLE_PADDING : 20, // Drag padding for dependency creation
        getImagesDir : function() {
            return "/" + common.Common.webAppContextPath() + "/zkau/web/ganttz/img/";
        }
    });

ganttz.DependencyComponent = zk.$extends(
    ganttz.DependencyComponentBase,
    {
        $define : {
            idTaskOrig : null,
            idTaskEnd : null,
            dependencyType : null
        },

        bind_ : function() {
            this.$supers('bind_', arguments);

            /* TODO Maybe move this listener to the $init method */
            YAHOO.util.Event.onDOMReady(this.proxy(function() {
                this.draw();
            }));
        },

        draw : function() {
            this._withOriginAndDestination(function(origin, destination) {
                var orig = this.findPos_(origin);
                var dest = this.findPos_(destination);

                // This corner case may depend on dependence type
                var offsetX = origin.outerWidth() - ganttz.TaskComponent.CORNER_WIDTH;
                var separation = orig.left + origin.outerWidth() - dest.left;

                if (separation > 0) {
                    offsetX = offsetX - separation;
                }
                if (this.getDependencyType() == this.$class.END_START || this.getDependencyType() == null) {
                    orig.left = orig.left + Math.max(0, offsetX);
                } else if (this.getDependencyType() == this.$class.END_END) {
                    orig.left = orig.left + origin.outerWidth();
                    dest.left = dest.left + destination.outerWidth();
                }

                orig.top = orig.top + ganttz.TaskComponent.HEIGHT;
                dest.top = dest.top + ganttz.TaskComponent.HALF_HEIGHT;

                if (orig.top > dest.top) {
                    orig.top = orig.top - ganttz.TaskComponent.HEIGHT;
                }

                this.drawArrow_(orig, dest);
            });
        },

        _withOriginAndDestination : function(f) {
            f.call(this, jq('#' + this.getIdTaskOrig()), jq('#' + this.getIdTaskEnd()));
        }
    },
    {

    });

ganttz.UnlinkedDependencyComponent = zk.$extends(
    ganttz.DependencyComponentBase,
    {
        $init : function() {
            this.$supers('$init', arguments);
            this._DOMlisttasks = jq('#listtasks');
            this._DOMlistdependencies = jq('#listdependencies');
            this._WGTganttpanel = ganttz.GanttPanel.getInstance();
        },

        bind_ : function() {
            this.$supers('bind_', arguments);
            /*
             * We use document.documentElement as the DOM element to attach this listener
             * because document.documentElement always gets the key events (in all browsers)
             */
            this.domListen_(document.documentElement,'onKeyup','_handleKeyUp');
            this._updateArrow();
        },

        unbind_ : function() {
            this.domUnlisten_(document.documentElement,'onKeyup','_handleKeyUp');
            this.domUnlisten_(this._WGTganttpanel.$n(), 'onMousemove', '_updateArrow');
            this.domUnlisten_(this._WGTganttpanel.$n(), 'onClick', '_consolidateDependency');
            this.$supers('unbind_', arguments);
        },

        draw : function() {
            this.domListen_(this._WGTganttpanel.$n(), 'onMousemove', '_updateArrow');
            this.domListen_(this._WGTganttpanel.$n(), 'onClick', '_consolidateDependency');
        },

        setOrigin : function(origin) {
            this._DOMorigin = jq(origin);
            this._WGTorigin = ganttz.TaskComponent.$(origin.id);
        },

        _consolidateDependency : function() {
            var dependency =  null;
            if ((dependency = this._isOverTask()) != null) {
                this._WGTorigin.consolidateNewDependency(dependency);
            }
            /* We remove the dependency line. If the user clicked over a task, a new dependecy line will be created. */
            ganttz.DependencyList.getInstance().removeChild(this);
        },

        _isOverTask : function() {
            var tasksOver = jq.grep(ganttz.TaskComponent.allTaskComponents(), function(task) {
                return task.mouseOverTask;
            });
            return tasksOver.length > 0 ? tasksOver[0] : null;
        },

        _getCoordOrigin: function() {
            if (this._coordOrigin) {
                return this._coordOrigin;
            }

            var coordOrigin = this.findPos_(this._DOMorigin);
            coordOrigin.left = coordOrigin.left + Math.max(0, this._DOMorigin.outerWidth() - ganttz.TaskComponent.CORNER_WIDTH);
            coordOrigin.top = coordOrigin.top + ganttz.TaskComponent.HEIGHT;

            return this._coordOrigin = coordOrigin;
        },

        _updateArrow: function () {
            this.drawArrow_(this._getCoordOrigin(), this._findCoordsForMouse());
        },

        _findCoordsForMouse: function() {
            var reference =  jq('#listdependencies').offset();
            return { left : this._WGTganttpanel.getXMouse() - reference.left, top: this._WGTganttpanel.getYMouse() - reference.top };
        },

        _handleKeyUp: function(event) {
            if ( event.keyCode != 27 ) {
                return;
            }
            event.stop();
            ganttz.DependencyList.getInstance().removeChild(this);
        }
    });

zk.afterLoad('ganttz',function() {
    ganttz.UnlinkedDependencyComponent.molds = ganttz.DependencyComponent.molds;
});
