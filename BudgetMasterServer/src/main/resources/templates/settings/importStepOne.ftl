<html>
    <head>
        <#import "../helpers/header.ftl" as header>
        <@header.globals/>
        <@header.header "BudgetMaster - ${locale.getString('settings.database.import')}"/>
        <#import "/spring.ftl" as s>
    </head>
    <@header.body>
        <#import "../helpers/navbar.ftl" as navbar>
        <@navbar.navbar "settings" settings/>

        <main>
            <div class="card main-card background-color">
                <div class="container">
                    <div class="section center-align">
                        <div class="headline">${locale.getString("info.title.database.import.dialog")}</div>
                    </div>
                </div>

                <div class="container">
                    <div class="section center-align">
                        <div class="headline-small">${locale.getString("info.database.import")}</div>
                    </div>
                </div>

                <div class="container">
                    <form name="Import" action="<@s.url '/settings/database/import/step2'/>" method="post">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                        <div class="row">
                            <div class="col s12 m10 offset-m1 l10 offset-l1 xl8 offset-xl2">
                                <table class="bordered">
                                    <#list database.getNumberOfEntitiesByType() as entityType, numberOfItems>
                                        <tr>
                                            <td>
                                                <label>
                                                    <input type="checkbox" checked="checked" name="${entityType.name()}" <#if entityType.getImportRequired().name() == "REQUIRED">disabled="disabled"</#if>>
                                                    <span></span>
                                                </label>
                                            </td>
                                            <td><i class="material-icons left">${entityType.getIcon()}</i>
                                                <div class="import-entity-name">${locale.getString(entityType.getLocalizationKey())}</div>
                                            </td>
                                            <td>${numberOfItems}</td>
                                            <td><a class="btn btn-flat text-default import-entity-help-button"
                                                   data-title="${locale.getString(entityType.getLocalizationKey())}"
                                                   data-text="${locale.getString("import.entity." + entityType.name()?lower_case)}"><i class="material-icons">help_outline</i></a>
                                            </td>
                                        </tr>
                                    </#list>
                                </table>
                            </div>
                        </div>

                        <div class="row">
                            <div class="col m6 l4 offset-l2 right-align">
                                <@header.buttonLink url='/settings' icon='clear' localizationKey='cancel' color='red'/>
                            </div>

                            <div class="col m6 l4 left-align">
                                <@header.buttonSubmit name='action' icon='unarchive' localizationKey='settings.database.import' id='buttonImport' color='green'/>
                            </div>
                        </div>
                    </form>

                    <div id="modal-import-entity-help" class="modal background-color">
                        <div class="modal-content">
                            <h4 id="modal-import-entity-help-title"></h4>
                            <p id="modal-import-entity-help-content"></p>
                        </div>
                        <div class="modal-footer background-color">
                            <@header.buttonLink url='' icon='done' localizationKey='ok' color='green' classes='modal-action modal-close text-white' noUrl=true/>
                        </div>
                    </div>
                </div>
            </div>
        </main>

        <!-- Scripts-->
        <#import "../helpers/scripts.ftl" as scripts>
        <@scripts.scripts/>
        <script src="<@s.url '/js/import.js'/>"></script>
    </@header.body>
</html>