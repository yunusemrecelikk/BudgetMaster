<html>
    <head>
        <#import "../header.ftl" as header>
        <@header.header/>
        <#assign locale = static["tools.Localization"]>
    </head>
    <body class="budgetmaster-blue-light">
        <#import "../navbar.ftl" as navbar>
        <@navbar.navbar "accounts"/>

        <main>
            <div class="card main-card">
                <div class="container">
                    <div class="section center-align">
                        <div class="grey-text text-darken-4 headline">${locale.getString("menu.accounts")}</div>
                    </div>
                </div>
                <br>
                <div class="center-align"><a href="/accounts/newAccount" class="waves-effect waves-light btn budgetmaster-blue"><i class="material-icons left">add</i>${locale.getString("title.account.new")}</a></div>
                <br>
                <div class="container">
                    <table class="bordered">
                        <#list accounts as account>
                        <tr>
                            <td>${account.getName()}</td>
                            <td>
                                <a href="/accounts/${account.getID()}/edit" class="btn-flat no-padding"><i class="material-icons left">edit</i></a>
                                <a href="/accounts/${account.getID()}/requestDelete" class="btn-flat no-padding"><i class="material-icons left">delete</i></a>
                            </td>
                        </tr>
                        </#list>
                    </table>
                </div>
            </div>

            <#if currentAccount??>
                <!-- confirm delete modal -->
                <div id="modalConfirmDelete" class="modal">
                    <div class="modal-content">
                        <h4>${locale.getString("info.title.account.delete")}</h4>
                        <p>${locale.getString("info.text.account.delete", currentAccount.getName(), currentAccount.getReferringPayments()?size)}</p>
                    </div>
                    <div class="modal-footer">
                        <a href="/accounts" class="modal-action modal-close waves-effect waves-red btn-flat ">${locale.getString("cancel")}</a>
                        <a href="/accounts/${currentAccount.getID()}/delete" class="modal-action modal-close waves-effect waves-green btn-flat ">${locale.getString("info.button.account.delete")}</a>
                    </div>
                </div>
            </#if>
        </main>

        <!--  Scripts-->
        <script src="https://code.jquery.com/jquery-2.1.1.min.js"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/materialize/0.100.2/js/materialize.min.js"></script>
        <script src="/js/main.js"></script>
        <script src="/js/categories.js"></script>
    </body>
</html>